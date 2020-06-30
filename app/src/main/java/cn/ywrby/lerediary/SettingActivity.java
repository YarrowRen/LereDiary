package cn.ywrby.lerediary;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import cn.ywrby.lerediary.db.Diary;
import cn.ywrby.lerediary.util.ZipUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.litepal.LitePal;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {


    private Context mContext;

    private final int SIMPLE_TYPE=1;
    private final int GENERAL_TYPE=2;
    private static final int FILE_SELECT_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mContext=SettingActivity.this;

        //设置ToolBar
        final Toolbar toolbar=findViewById(R.id.setting_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();  //这里的ActionBar实际上已经是通过ToolBar实现
        if(actionBar!=null){
            //Toolbar最左侧的按钮被称为HomeAsUp按钮，默认图标是返回箭头，作用就是返回上一个活动
            actionBar.setDisplayHomeAsUpEnabled(true);  //允许显示返回按钮
        }

        TextView setting_simple=findViewById(R.id.setting_simple);
        setting_simple.setOnClickListener(this);
        TextView setting_card=findViewById(R.id.setting_card);
        setting_card.setOnClickListener(this);
        TextView setting_notice=findViewById(R.id.setting_notice);
        setting_notice.setOnClickListener(this);
        TextView setting_input=findViewById(R.id.setting_input);
        setting_input.setOnClickListener(this);
        TextView setting_output=findViewById(R.id.setting_output);
        setting_output.setOnClickListener(this);
        TextView setting_open_source=findViewById(R.id.setting_open_source);
        setting_open_source.setOnClickListener(this);
        TextView setting_opinion=findViewById(R.id.setting_opinion);
        setting_opinion.setOnClickListener(this);
        TextView setting_about=findViewById(R.id.setting_about);
        setting_about.setOnClickListener(this);




    }

    /**
     * 展示提醒设置界面，用于设置提醒或删除提醒
     */
    private void showClock(){
        View clockDialogView=View.inflate(mContext,R.layout.clock_item,null);
        //创建自定义选择框
        final AlertDialog.Builder clockDialogBuilder = new AlertDialog.Builder(mContext);
        clockDialogBuilder
                .setView(clockDialogView)
                .create();
        final AlertDialog clockDialog=clockDialogBuilder.show();
        Window clockWin=clockDialog.getWindow();
        clockWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = clockWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        clockWin.setAttributes(params);
        Button clock_true=clockWin.findViewById(R.id.clock_true);
        Button clock_delete=clockWin.findViewById(R.id.clock_delete);


        //设置定时广播
        final AlarmManager alarmManager=(AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i=new Intent(this,ClockService.class);
        final PendingIntent pi=PendingIntent.getService(this,0,i,0);
        final int aDay=60*60*24*1000;  //一天的毫秒数
        final long triigerAtTime=System.currentTimeMillis()+aDay;  //设置定时器开启时间为一天后

        clock_true.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                clockDialog.dismiss();
                Toast.makeText(mContext,"日记提醒设置成功！",Toast.LENGTH_SHORT).show();
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,triigerAtTime,aDay,pi);  //设定重复计时器，显示响应的通知
            }
        });
        clock_delete.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                clockDialog.dismiss();
                Toast.makeText(mContext,"已取消日记提醒！",Toast.LENGTH_SHORT).show();
                alarmManager.cancel(pi);  //取消计时器
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                Intent intent=new Intent(SettingActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent=new Intent(SettingActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.setting_simple:
                SharedPreferences.Editor editor2=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                editor2.putInt("TYPE",SIMPLE_TYPE);
                editor2.apply();
                Intent intent2=new Intent(mContext,MainActivity.class);
                startActivity(intent2);
                finish();
                break;
            case R.id.setting_card:
                SharedPreferences.Editor editor=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                editor.putInt("TYPE",GENERAL_TYPE);
                editor.apply();
                Intent intent1=new Intent(mContext,MainActivity.class);
                startActivity(intent1);
                finish();
                break;
            case R.id.setting_notice:
                showClock();
                break;
            case R.id.setting_output:
                //导出日记文件
                showBackUp();
                break;
            case R.id.setting_input:
                //导入日记文件
                showFileChooser();
                break;
            case R.id.setting_open_source:
                Intent intent=new Intent(SettingActivity.this,OpenSourceLibraryActivity.class);
                startActivity(intent);
                break;
            case R.id.setting_opinion:
                Intent intent3=new Intent(SettingActivity.this,FeedbackActivity.class);
                startActivity(intent3);
                break;
            case R.id.setting_about:
                Intent intent4=new Intent(SettingActivity.this,AboutActivity.class);
                startActivity(intent4);
                break;
            default:
                break;


        }
    }


    /**
     * 询问是否确定备份全部日记信息
     */
    private void showBackUp(){
        View backupDialogView=View.inflate(mContext,R.layout.back_up,null);
        //创建自定义选择框
        final AlertDialog.Builder backupDialogBuilder = new AlertDialog.Builder(mContext);
        backupDialogBuilder
                .setView(backupDialogView)
                .create();
        final AlertDialog backupDialog=backupDialogBuilder.show();
        Window backupWin=backupDialog.getWindow();
        backupWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = backupWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        backupWin.setAttributes(params);
        Button backup_true=backupWin.findViewById(R.id.backup_true);
        Button backup_cancel=backupWin.findViewById(R.id.backup_cancel);


        backup_true.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                backupDialog.dismiss();
                //读取数据库内容，并且按照格式生成json格式文件
                Gson gson=new Gson();
                List<Diary> diaries= LitePal.findAll(Diary.class);
                StringBuilder jsonArray = new StringBuilder();
                jsonArray.append("[");  //因为有多个日记对象，所以生成json数组的文件形式，开头添加中括号
                for(Diary diary:diaries){
                    jsonArray.append(gson.toJson(diary));  //将Diary对象转换为json对象
                    jsonArray.append(",");
                }
                jsonArray.deleteCharAt(jsonArray.length()-1);  //删除最后一个对象末尾多加的","
                jsonArray.append("]");
                //写入文件
                try {
                    FileOutputStream fos = openFileOutput("backup.json",MODE_PRIVATE);
                    fos.write(jsonArray.toString().getBytes());
                    fos.close();
                    Log.d("YWRBY1", "onNoDoubleClick: "+"成功");
                } catch (Exception e) {
                    Log.d("YWRBY1", "onNoDoubleClick: "+"失败");
                    Toast.makeText(SettingActivity.this,"备份失败，请稍后重试",Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                //创建backup文件夹用于将数据库文件和图片文件放在同一文件夹下，方便打包
                File pack=new File(getExternalCacheDir()+"/backup");
                pack.mkdir();
                try {
                    //压缩图片内容
                    ZipUtil.zip(String.valueOf(getExternalFilesDir("photo")),getExternalCacheDir()+"/backup/photo.zip");
                    //压缩数据库内容
                    ZipUtil.zip(getFilesDir()+"/backup.json",getExternalCacheDir()+"/backup/diary.zip");
                    //保存压缩包的路径，默认保存到系统下载文件夹下，方便查找
                    String path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/backup.zip";
                    //整体压缩成为一个压缩包，方便分享或保存
                    ZipUtil.zip(getExternalCacheDir()+"/backup",path);
                    //关闭窗口
                    backupDialog.dismiss();
                    //询问是否将备份文件进行分享
                    showShareBackUp(path);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(SettingActivity.this,"备份失败，请稍后重试",Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
        //取消
        backup_cancel.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                backupDialog.dismiss();
            }
        });
    }


    /**
     * 询问是否分享已经备份好的文件
     * @param path 备份文件路径
     */
    private void showShareBackUp(String path){
        View shareBackupDialogView=View.inflate(mContext,R.layout.share_backup,null);
        //创建自定义选择框
        final AlertDialog.Builder shareBackupDialogBuilder = new AlertDialog.Builder(mContext);
        shareBackupDialogBuilder
                .setView(shareBackupDialogView)
                .create();
        final AlertDialog shareBackupDialog=shareBackupDialogBuilder.show();
        Window shareBackupWin=shareBackupDialog.getWindow();
        shareBackupWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = shareBackupWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        shareBackupWin.setAttributes(params);

        TextView share_backup_path=shareBackupDialogView.findViewById(R.id.share_backup_path);
        share_backup_path.setText("文件已成功保存，路径："+path);
        Button share_backup_true=shareBackupDialogView.findViewById(R.id.share_backup_true);
        Button share_backup_share=shareBackupDialogView.findViewById(R.id.share_backup_share);
        //不分享，点击确定
        share_backup_true.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBackupDialog.dismiss();
            }
        });
        //将压缩包进行分享
        final File file=new File(path);
        share_backup_share.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                //分享事件
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                //针对不同版本，采用不同方式获取文件Uri
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getPackageName()+".fileprovider", file);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }else {
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                }
                // 指定发送内容的类型 (MIME type)
                sendIntent.setType("application/zip");
                mContext.startActivity(Intent.createChooser(sendIntent,"Share to..."));
                shareBackupDialog.dismiss();
            }
        });
    }

    /**
     * 打开系统文件浏览器，利用startActivityForResult获取选中项的地址
     */
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 针对高版本利用Uri进行真实地址的解析
     * @param uri 文件Uri
     */
    @TargetApi(19)
    private void handleOnKitKat(Uri uri){
        String path=null;
        Log.d("YWRBY1", "handleImageOnKitKat: "+uri.toString());
        if(DocumentsContract.isDocumentUri(this,uri)){
            String docID=DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id=docID.split(":")[1];
                String selection= MediaStore.Images.Media._ID+"="+id;
                path=getPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri= ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docID));
                path=getPath(contentUri,null);
            }
        }else if("content".equalsIgnoreCase(uri.getScheme())){
            path=getPath(uri,null);
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            path=uri.getPath();
        }
        importDiary(path);
    }


    /**
     * 针对低版本进行文件真实地址解析
     * @param uri 文件Uri
     */
    private void handleBeforeKitKat(Uri uri){
        String path=getPath(uri,null);
        importDiary(path);
    }


    /**
     * 利用文件真实Uri获取文件地址
     * @param uri 文件真实Uri
     * @param selection
     * @return 文件地址
     */
    private String getPath(Uri uri,String selection){
        String path=null;
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if(cursor!=null){
            if(cursor.moveToNext()){
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * 恢复日记内容：解压缩备份文件，将数据插入数据库，将图片添加回图片文件夹
     * @param path 备份的压缩文件路径
     */
    private void importDiary(String path){
        try {
            //解压缩外层
            ZipUtil.unzip(path, String.valueOf(getExternalCacheDir()));
            //加压缩图片内容至图片文件夹
            ZipUtil.unzip(getExternalCacheDir()+"/photo.zip", String.valueOf(getExternalFilesDir("photo")));
            //加压缩日记数据库内容为json格式
            ZipUtil.unzip(getExternalCacheDir()+"/diary.zip", String.valueOf(getExternalCacheDir()));

            //json格式数据解析
            Gson gson=new Gson();
            //读取文件
            File file=new File(getExternalCacheDir()+"/backup.json");
            StringBuilder stringBuilder = new StringBuilder();
            InputStream instream = new FileInputStream(file);
            //使用IO流读取json文件内容
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(instream,"utf-8"));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();
            } catch (IOException e) {
                Log.d("ywrby1", "importDiary: "+"写入失败");
                e.printStackTrace();
            }
            String jsonData=stringBuilder.toString();
            //解析数据
            ArrayList<Diary> diaries=new ArrayList<Diary>();
            Type listType = new TypeToken<List<Diary>>() {}.getType();
            diaries=gson.fromJson(jsonData,listType);
            for(Diary d:diaries){
                //保存Diary对象
                Diary diary=new Diary();
                diary.setCover(d.getCover());
                diary.setUuid(d.getUuid());
                diary.setWeather(d.getWeather());
                diary.setDate(d.getDate());
                diary.setTime(d.getTime());
                diary.setContent(d.getContent());
                diary.setTitle(d.getTitle());
                diary.save();
            }
            //重新打开主页面，刷新数据库内容
            Intent intent=new Intent(mContext,MainActivity.class);
            startActivity(intent);
            finish();
        } catch (IOException e) {
            Log.d("YWRBY1", "importDiary: 失败");
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    // Get the path
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleOnKitKat(uri);
                    } else {
                        handleBeforeKitKat(uri);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
