package cn.ywrby.lerediary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import cn.ywrby.lerediary.db.Diary;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.widemouth.library.wmview.WMTextEditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static com.widemouth.library.wmview.WMTextEditor.TYPE_NON_EDITABLE;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.ViewHolder> {


    private Context mContext;
    private final List<Diary> mDiaryList;  //日记列表，被应用于RecyclerView中展示
    private int viewWidth=0;  //整体视图宽度，用于确定封面图片的大小

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;  //日记视图
        TextView item_date;  //日期
        ImageView item_weather;  //天气
        ImageView item_cover;  //封面
        TextView item_title;  //标题
        WMTextEditor item_content;  //内容
        ImageView item_edit;  //编辑按钮
        ImageView item_collect;  //收藏按钮
        ImageView item_forward;  //转发按钮
        TextView item_time;  //时间


        public ViewHolder(@NonNull View view) {
            super(view);
            cardView = (CardView) view;
            item_date = view.findViewById(R.id.item_date);
            item_weather = view.findViewById(R.id.item_weather);
            item_cover = view.findViewById(R.id.item_cover);
            item_title = view.findViewById(R.id.item_title);
            item_content = view.findViewById(R.id.item_content);
            item_edit = view.findViewById(R.id.item_edit);
            item_forward = view.findViewById(R.id.item_forward);
            item_time = view.findViewById(R.id.item_time);

        }
    }

    public DiaryAdapter(List<Diary> diaryList) {
        mDiaryList = diaryList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        //绑定事件
        View view = LayoutInflater.from(mContext).inflate(R.layout.diary_item, parent, false);

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
        //编辑当前日记
        viewHolder.item_edit.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                int position=viewHolder.getAdapterPosition();
                Diary diary=mDiaryList.get(position);
                Intent intent=new Intent(mContext,EditDiaryActivity.class);
                intent.putExtra("diary",diary);
                mContext.startActivity(intent);
                ((Activity)mContext).finish();
            }
        });
        //转发该条日记
        viewHolder.item_forward.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                shareDiaryPic(viewHolder);
            }
        });

        return viewHolder;
    }

    //绑定控件，尽量避免在此处加载耗时操作
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        final Diary diary = mDiaryList.get(position);
        //加载日期
        holder.item_date.setText(diary.getDate());
        //加载天气
        Glide.with(holder.cardView).load(initWeather(position)).override(60,60).into(holder.item_weather);
        //获取封面路径
        final String path=diary.getCover();

        //只在第一次测量view的宽度，避免每次测量耗时导致滑动卡顿
        if(viewWidth==0) {
            holder.cardView.post(new Runnable() {
                @Override
                public void run() {
                    viewWidth = holder.cardView.getWidth();
                    Glide.with(holder.cardView)
                            .load(path)
                            .override(viewWidth, (int) (viewWidth * 0.5))//限制大小
                            .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(20)))//设置圆角和裁切图片
                            .into(holder.item_cover);
                }
            });
        }else{
            Glide.with(holder.cardView)
                    .load(path)
                    .override(viewWidth, (int) (viewWidth * 0.5))//限制大小
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(20)))//设置圆角和裁切图片
                    .into(holder.item_cover);
        }
        //加载标题
        holder.item_title.setText(diary.getTitle());
        //设置正文格式和内容（只读，最长5行）
        holder.item_content.setMaxLines(5);
        holder.item_content.fromHtml(diary.getContent());
        holder.item_content.setEditorType(TYPE_NON_EDITABLE);
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


    /**
     * 通过Android原生的分享功能，结合截图功能，进行当前日记卡片的分享
     * @param viewHolder 用来获取卡片对象
     */
    private void shareDiaryPic(ViewHolder viewHolder){
        int position=viewHolder.getAdapterPosition();
        View view=viewHolder.cardView;

        //生成view的截图
        view.setDrawingCacheEnabled(true);
        view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        view.setDrawingCacheBackgroundColor(Color.WHITE);
        Bitmap cacheBitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(cacheBitmap);
        canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        String fileName=mContext.getExternalCacheDir().toString()+"/lere_forward_"+System.currentTimeMillis()+".jpg";
        File filePic = new File(fileName);
        try {
            FileOutputStream fos = new FileOutputStream(filePic);
            cacheBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Log.d("YWRBY1", "onGlobalLayout: "+"成功执行");
        }catch (IOException e){
            Log.d("YWRBY1", "onGlobalLayout: "+e.getMessage());
            e.printStackTrace();
        }

        //分享事件
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        //针对不同版本，采用不同方式获取文件Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getPackageName()+".fileprovider", filePic);
            sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }else {
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(filePic));
        }
        // 指定发送内容的类型 (MIME type)
        sendIntent.setType("image/jpeg");
        mContext.startActivity(Intent.createChooser(sendIntent,"Share to..."));
    }


    @Override
    public int getItemCount() {
        return mDiaryList.size();
    }




}
