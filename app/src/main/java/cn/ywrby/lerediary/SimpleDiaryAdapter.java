package cn.ywrby.lerediary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Vibrator;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import cn.ywrby.lerediary.db.Diary;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;

public class SimpleDiaryAdapter extends RecyclerView.Adapter<SimpleDiaryAdapter.ViewHolder> {
    private Context mContext;
    private final List<Diary> mDiaryList;  //日记列表，被应用于RecyclerView中展示
    private int viewWidth=0;  //整体视图宽度，用于确定封面图片的大小

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout cardView;  //日记视图
        TextView item_date;  //日期
        ImageView item_weather;  //天气
        ImageView item_cover;  //封面
        TextView item_title;  //标题
        TextView item_time;  //时间


        public ViewHolder(@NonNull View view) {
            super(view);
            cardView = (LinearLayout) view;
            item_date = view.findViewById(R.id.simple_item_date);
            item_weather = view.findViewById(R.id.simple_item_weather);
            item_cover = view.findViewById(R.id.simple_item_cover);
            item_title = view.findViewById(R.id.simple_item_title);
            item_time = view.findViewById(R.id.simple_item_time);

        }
    }

    public SimpleDiaryAdapter(List<Diary> diaryList) {
        mDiaryList = diaryList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        //绑定事件
        View view = LayoutInflater.from(mContext).inflate(R.layout.simple_diary_item, parent, false);

        final ViewHolder viewHolder=new ViewHolder(view);

        //跳转到展示单独日记页面
        viewHolder.cardView.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                int position=viewHolder.getAdapterPosition();
                Diary diary=mDiaryList.get(position);
                Intent intent=new Intent(mContext,ViewDiaryActivity.class);
                intent.putExtra("diary",diary);
                mContext.startActivity(intent);
                ((Activity)mContext).finish();
            }
        });

        //删除当前日记
        viewHolder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position=viewHolder.getAdapterPosition();
                Diary diary=mDiaryList.get(position);
                showDelete(v,diary,position);
                return false;
            }
        });

        return viewHolder;
    }

    /**
     * 绑定控件，尽量避免在此处加载耗时操作
     * @param holder
     * @param position Diary对象在列表中的索引值
     */
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        final Diary diary = mDiaryList.get(position);
        //加载日期
        holder.item_date.setText(diary.getDate());
        //加载天气
        Glide.with(holder.cardView).load(initWeather(position)).override(60,60).into(holder.item_weather);
        //获取封面路径
        final String path=diary.getCover();


        Glide.with(holder.cardView)
                .load(path)
                .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(20)))//设置圆角和裁切图片
                .into(holder.item_cover);

        //加载标题
        holder.item_title.setText(diary.getTitle());
        //加载时间
        holder.item_time.setText(diary.getTime());

    }



    /**
     * 获取天气信息
     * @param position 当前日记的索引值
     * @return 天气图片的ID
     */
    private int initWeather(int position){
        Diary diary = mDiaryList.get(position);
        int weather = diary.getWeather();
        switch (weather) {
            case 1:
                return R.drawable.mood1;
            case 2:
                return R.drawable.mood2;
            case 3:
                return R.drawable.mood3;
            case 4:
                return R.drawable.mood4;
            case 5:
                return R.drawable.mood5;
            case 6:
                return R.drawable.mood6;
            case 7:
                return R.drawable.mood7;
            case 8:
                return R.drawable.mood8;
            default:
                break;
        }
        return 0;
    }

    /**
     * 弹出删除提示框
     * @param view 所在视图
     * @param diary 要删除的日记对象（从数据库中清除对象时使用）
     * @param position 日记对象在展示列表中的索引值（从列表中移除对象时使用）
     */
    private void showDelete(final View view, final Diary diary, final int position){
        //震动提示
        Vibrator vibrator = (Vibrator)view.getContext().getSystemService(view.getContext().VIBRATOR_SERVICE);
        vibrator.vibrate(100);
        //匹配视图
        View deleteDialogView=View.inflate(view.getContext(),R.layout.delete_diary_item,null);
        //创建自定义选择框
        final AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(view.getContext());
        deleteDialogBuilder
                .setView(deleteDialogView)
                .create();
        final AlertDialog deleteDialog=deleteDialogBuilder.show();
        Window deleteWin=deleteDialog.getWindow();
        deleteWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = deleteWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        deleteWin.setAttributes(params);
        //设置响应事件
        Button delete_true=deleteWin.findViewById(R.id.delete_true);

        delete_true.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diary.delete();  //在数据库中删除当前数据
                mDiaryList.remove(position);   //在展示列表上删除当前数据（不再在列表中展示）
                notifyDataSetChanged();  //提示数据已经发生改变
                deleteDialog.dismiss();
            }
        });
        Button delete_cancel=deleteWin.findViewById(R.id.delete_cancel);
        delete_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog.dismiss();
            }
        });

    }

    @Override
    public int getItemCount() {
        return mDiaryList.size();
    }


}
