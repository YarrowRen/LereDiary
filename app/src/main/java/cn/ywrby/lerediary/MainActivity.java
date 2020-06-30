package cn.ywrby.lerediary;


import android.annotation.TargetApi;
import android.app.*;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
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
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import cn.ywrby.lerediary.db.Diary;
import cn.ywrby.lerediary.util.ZipUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.litepal.LitePal;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;  //刷新日记
    //从数据库读取日记信息
    private List<Diary> diaryList;
    private RecyclerView.Adapter adapter;

    private final int SIMPLE_TYPE=1;
    private final int GENERAL_TYPE=2;
    private int TYPE;


    private static final int FILE_SELECT_CODE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext=MainActivity.this;

        //设置导航栏滑出后占屏幕大小，这里设置滑出后占屏65%
        navigationView=findViewById(R.id.nav_view);
        ViewGroup.LayoutParams params = navigationView.getLayoutParams();
        params.width = (int)(( getResources().getDisplayMetrics().widthPixels )*0.65);
        navigationView.setLayoutParams(params);

        Toolbar toolbar=findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout=findViewById(R.id.drawer_layout);
        /*
        为mDrawerLayout添加监听器
        现在大部分安卓手机在滑动边缘时都是默认返回上一个活动，如果在这种情况下还采取滑动打开的方式
        就会导致两个功能冲突，影响应用使用体验
        这里规定在滑动窗口开启的情况下解锁滑动模式，允许用户通过滑动关闭滑窗
        在滑窗隐藏的情况下，禁止用户通过滑动边缘打开滑窗，只能通过HomeAsUp按钮
         */
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            @Override
            public void onDrawerStateChanged(int newState) { }
        });
        ActionBar actionBar=getSupportActionBar();  //这里的ActionBar实际上已经是通过ToolBar实现
        if(actionBar!=null){
            //Toolbar最左侧的按钮被称为HomeAsUp按钮，默认图标是返回箭头，作用就是返回上一个活动
            //这里已经对它的样式和功能都做了修改
            //（功能的修改体现在onOptionsItemSelected中，将其功能指定为打开滑动菜单界面）
            actionBar.setDisplayHomeAsUpEnabled(true);  //允许显示导航按钮
            actionBar.setHomeAsUpIndicator(R.drawable.list);    //设置导航按钮图标
        }


        /* *******************************************************************************************
         *
         * 控制侧边栏
         *
         ********************************************************************************************/

        NavigationView navView=findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.nav_simple:
                        //切换简单模式
                        SharedPreferences.Editor editor2=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                        editor2.putInt("TYPE",SIMPLE_TYPE);
                        editor2.apply();
                        Intent intent2=new Intent(mContext,MainActivity.class);
                        startActivity(intent2);
                        mDrawerLayout.closeDrawers();
                        finish();
                        break;
                    case R.id.nav_card:
                        //切换卡片模式
                        SharedPreferences.Editor editor=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                        editor.putInt("TYPE",GENERAL_TYPE);
                        editor.apply();
                        Intent intent1=new Intent(mContext,MainActivity.class);
                        startActivity(intent1);
                        mDrawerLayout.closeDrawers();
                        finish();
                        break;
                    case R.id.nav_notice:
                        //设定每日提醒
                        showClock();
                        break;
                    case R.id.nav_output:
                        //导出日记文件
                        showBackUp();
                        break;
                    case R.id.nav_input:
                        //导入日记文件
                        showFileChooser();
                        break;
                    case R.id.nav_open_source:
                        //开源情况说明
                        Intent intent=new Intent(mContext,OpenSourceLibraryActivity.class);
                        startActivity(intent);
                        mDrawerLayout.closeDrawers();
                        break;
                    case R.id.nav_opinion:
                        //意见反馈方式
                        Intent intent3=new Intent(mContext,FeedbackActivity.class);
                        startActivity(intent3);
                        mDrawerLayout.closeDrawers();
                        break;
                    case R.id.nav_about:
                        //关于软件
                        Intent intent4=new Intent(mContext,AboutActivity.class);
                        startActivity(intent4);
                        mDrawerLayout.closeDrawers();
                        break;
                }
                return true;
            }
        });


        /* *******************************************************************************************
         *
         * 操作按钮 设置点击事件
         *
         ********************************************************************************************/

        FloatingActionButton fabWrite=findViewById(R.id.fab);
        fabWrite.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                Intent intent=new Intent(mContext,EditDiaryActivity.class);
                startActivity(intent);
                finish();  //这里结束当前活动，方便在保存日记回到该页面时重新调用onCreate方法进而刷新日记内容
            }
        });


        /* *******************************************************************************************
         *
         * 读取规定的布局方式
         *
         ********************************************************************************************/

        recyclerView=findViewById(R.id.recycler_view);
        initDiary();  //在创建时初始化日记内容
        //绑定控制器和适配器
        GridLayoutManager layoutManager=new GridLayoutManager(this,1);
        SharedPreferences pref=getSharedPreferences("TYPE",MODE_PRIVATE);
        TYPE=pref.getInt("TYPE",GENERAL_TYPE);
        switch (TYPE){
            case GENERAL_TYPE:
                /*
                //生命为瀑布流的布局方式，2列，布局方向为垂直
                StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
                //解决item跳动
                manager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
                recyclerView.setLayoutManager(manager);
                 */
                adapter=new DiaryAdapter(diaryList);
                break;
            case SIMPLE_TYPE:
                adapter=new SimpleDiaryAdapter(diaryList);
                break;
        }

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);

        /* *******************************************************************************************
         *
         * 刷新日记内容
         *
         ********************************************************************************************/


        //下拉刷新
        swipeRefreshLayout=findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeColors(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshDiary();
            }
        });


    }


    /*
    初始化活动的菜单项内容，只在菜单项被放置时调用一次
    getMenuInflater方法得到MenuInflater对象
    MenuInflater类主要用于将menuXML文件转换为Menu对象
    调用其的inflate方法可以给当前活动创建菜单
    第一个参数指定通过哪一个资源文件创建菜单，第二个参数指定菜单项将添加到哪个Menu对象中
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true; //返回true表示允许创建的菜单显示出来，返回false则菜单不显示
    }



    /**
     * 设置系统状态栏的颜色
     * @param activity 当前活动，通过当前活动获得窗体
     * @param statusColor 要设置的状态栏颜色
     */
    static void setStatusBarColor(Activity activity, int statusColor) {
        Window window = activity.getWindow();
        //取消状态栏透明
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //添加Flag把状态栏设为可绘制模式
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        window.setStatusBarColor(statusColor);
        //设置系统状态栏处于可见状态
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        //让view不根据系统窗口来调整自己的布局
        ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null) {
            ViewCompat.setFitsSystemWindows(mChildView, false);
            ViewCompat.requestApplyInsets(mChildView);
        }
    }


    /**
     * 初始化日记展示内容
     */
    private void initDiary(){

        //在初次使用应用时，向数据库导入四篇介绍性文章
        SharedPreferences preferences=getSharedPreferences("DATA",MODE_PRIVATE);
        if(preferences.getBoolean("FIRST",true)){
            LitePal.getDatabase();  //创建数据库
            Gson gson=new Gson();
            String jsonData=getJson(mContext,"defaultDiary.json");
            ArrayList<Diary> diaries=new ArrayList<Diary>();
            Type listType = new TypeToken<List<Diary>>() {}.getType();
            diaries=gson.fromJson(jsonData,listType);
            for(Diary d:diaries){
                d.setUuid(UUID.randomUUID().toString());
                d.setCover(null);
                d.save();
            }
            SharedPreferences.Editor editor=preferences.edit();
            editor.putBoolean("FIRST",false);
            editor.apply();
        }

        /*
        当直接将从数据库或者其他方式获取的数据源集合或者数组直接赋值给当前数据源时，
        相当于当前数据源的对象发生了变化，当前对象已经不是adapter中的对象了，
        所以adaper调用notifyDataSetChanged()方法不会进行刷新数据和界面的操作

        简言之，直接从数据库获取到的资源集合是一个全新的对象，将这个新对象赋给列表，虽然改变了列表的值，但实际是创建了一个新列表
        所以在调用notifyDataSetChanged时，检查的还是原来的列表中的值，原来列表中的值没有改变，自然不会有更新效果
        所以这里在初始化日记列表时，要首先进行判断，如果是初次赋值，直接赋值即可。如果已经有值
        必须先清空列表，然后调用addAll添加值，保证对象不会改变
         */
        if(diaryList!=null) {
            diaryList.clear();
            diaryList.addAll(LitePal.order("date desc,time desc").find(Diary.class));
        }else {
            diaryList=LitePal.order("date desc,time desc").find(Diary.class);
        }


    }


    /**
     * 读取json文件（位于assets文件夹中的json文件）
     * @param context 当前Context
     * @param fileName 文件名称
     * @return 读取json文件得到的字符串对象（用于解析json）
     */
    public static String getJson(Context context, String fileName){
        StringBuilder stringBuilder = new StringBuilder();
        //获得assets资源管理器
        AssetManager assetManager = context.getAssets();
        //使用IO流读取json文件内容
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName),"utf-8"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }


    /**
     * 刷新日记展示内容
     */
    private void refreshDiary(){
        initDiary();  //初始化日记列表
        adapter.notifyDataSetChanged();  //提示适配器检查数据，数据已改变
        swipeRefreshLayout.setRefreshing(false);  //关闭刷新状态
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
                List<Diary> diaries=LitePal.findAll(Diary.class);
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
                    Toast.makeText(MainActivity.this,"备份失败，请稍后重试",Toast.LENGTH_SHORT).show();
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
                    String path=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/backup.zip";
                    //整体压缩成为一个压缩包，方便分享或保存
                    ZipUtil.zip(getExternalCacheDir()+"/backup",path);
                    //关闭窗口
                    backupDialog.dismiss();
                    //询问是否将备份文件进行分享
                    showShareBackUp(path);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(MainActivity.this,"备份失败，请稍后重试",Toast.LENGTH_SHORT).show();
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

    /**
     * 对菜单的响应事件
     * @param item 菜单选项
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            //修改HomeAsUp按钮的功能
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            //时钟功能
            case R.id.clock:
                showClock();
                break;
            //写日记功能
            case R.id.write_diary_menu:
                Intent intent=new Intent(mContext,EditDiaryActivity.class);
                startActivity(intent);
                finish();  //这里结束当前活动，方便在保存日记回到该页面时重新调用onCreate方法进而刷新日记内容
                break;
            //简单布局
            case R.id.simple_adapter_menu:
                SharedPreferences.Editor editor2=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                editor2.putInt("TYPE",SIMPLE_TYPE);
                editor2.apply();
                Intent intent2=new Intent(mContext,MainActivity.class);
                startActivity(intent2);
                finish();
                break;
            //卡片布局
            case R.id.adapter_menu:
                SharedPreferences.Editor editor=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                editor.putInt("TYPE",GENERAL_TYPE);
                editor.apply();
                Intent intent1=new Intent(mContext,MainActivity.class);
                startActivity(intent1);
                finish();
                break;
            //设置
            case R.id.setting_menu:
                Intent intent4=new Intent(mContext,SettingActivity.class);
                startActivity(intent4);
                finish();
                break;
            //意见功能
            case R.id.opinion_menu:
                Intent intent3=new Intent(mContext,FeedbackActivity.class);
                startActivity(intent3);
                break;

            default:
        }
        return true;
    }






}
