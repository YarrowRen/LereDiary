package cn.ywrby.lerediary;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import cn.ywrby.lerediary.db.Diary;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.widemouth.library.wmview.WMTextEditor;
import org.litepal.LitePal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.widemouth.library.wmview.WMTextEditor.TYPE_NON_EDITABLE;

public class ViewDiaryActivity extends AppCompatActivity {

    private Context mContext;
    private ImageView view_weather;
    private TextView view_date;
    private WMTextEditor view_content;
    private ImageView view_cover;
    private FloatingActionButton view_edit;

    private Diary diary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);

        mContext=ViewDiaryActivity.this;

        //获取传入的日记对象
        Intent intent=getIntent();
        diary=intent.getParcelableExtra("diary");

        //初始化控件对象
        view_weather=findViewById(R.id.view_weather);
        view_date=findViewById(R.id.view_date);
        view_content=findViewById(R.id.view_content);
        view_cover=findViewById(R.id.view_cover);
        view_edit=findViewById(R.id.view_edit);

        //载入信息
        view_weather.setImageResource(initWeather(diary.getWeather()));
        view_date.setText(diary.getDate()+"-"+diary.getTime());
        view_content.fromHtml(diary.getContent());
        view_content.setEditorType(TYPE_NON_EDITABLE);
        //响应编辑信息
        view_edit.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                Intent edit_intent=new Intent(mContext,EditDiaryActivity.class);
                edit_intent.putExtra("diary",diary);
                startActivity(edit_intent);
                finish();
            }
        });

        Toolbar toolbar=findViewById(R.id.view_toolbar);
        CollapsingToolbarLayout collapsingToolbarLayout=findViewById(R.id.view_collapsing_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        //返回键
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //标题
        collapsingToolbarLayout.setTitle(diary.getTitle());
        //显示封面
        String coverPath=diary.getCover();
        if(coverPath!=null) {
            Glide.with(this).load(coverPath).into(view_cover);
        }else {
            //使用默认的图片作为封面
            Glide.with(this).load(R.drawable.default_cover).into(view_cover);
        }

    }

    /**
     * 获取天气信息
     * @param weather 当前日记的索引值
     * @return 天气图片的ID
     */
    private int initWeather(int weather){
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
     * 弹出删除对话框，删除当前日记并退回到主页面
     */
    private void showDelete(){
        //匹配视图
        View deleteDialogView=View.inflate(mContext,R.layout.delete_diary_item,null);
        //创建自定义选择框
        final AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(mContext);
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
                LitePal.deleteAll(Diary.class,"uuid=?",diary.getUuid());
                deleteDialog.dismiss();
                Intent intent_back=new Intent(mContext,MainActivity.class);
                startActivity(intent_back);
                finish();
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
     * 通过Android原生的分享功能，结合截图功能，进行当前日记的分享
     */
    private void shareDiaryPic(){
        View view=findViewById(R.id.view_coordinator);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_view,menu);
        return true; //返回true表示允许创建的菜单显示出来，返回false则菜单不显示
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                Intent intent=new Intent(mContext,MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.view_delete:
                showDelete();
                break;
            case R.id.view_share:
                shareDiaryPic();
                break;
            case R.id.view_edit:
                Intent edit_intent=new Intent(mContext,EditDiaryActivity.class);
                edit_intent.putExtra("diary",diary);
                startActivity(edit_intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent=new Intent(mContext,MainActivity.class);
        startActivity(intent);
        finish();
    }
}
