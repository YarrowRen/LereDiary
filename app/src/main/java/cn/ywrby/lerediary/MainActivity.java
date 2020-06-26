package cn.ywrby.lerediary;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import cn.ywrby.lerediary.db.Diary;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.litepal.LitePal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

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




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
         * 操作按钮 设置点击事件
         *
         ********************************************************************************************/

        FloatingActionButton fabWrite=findViewById(R.id.fab);
        fabWrite.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                Intent intent=new Intent(MainActivity.this,EditDiaryActivity.class);
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


    //设置系统状态栏的颜色
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

    //初始化日记展示内容
    /*
    当直接将从数据库或者其他方式获取的数据源集合或者数组直接赋值给当前数据源时，
    相当于当前数据源的对象发生了变化，当前对象已经不是adapter中的对象了，
    所以adaper调用notifyDataSetChanged()方法不会进行刷新数据和界面的操作

    简言之，直接从数据库获取到的资源集合是一个全新的对象，将这个新对象赋给列表，虽然改变了列表的值，但实际是创建了一个新列表
    所以在调用notifyDataSetChanged时，检查的还是原来的列表中的值，原来列表中的值没有改变，自然不会有更新效果
    所以这里在初始化日记列表时，要首先进行判断，如果是初次赋值，直接赋值即可。如果已经有值
    必须先清空列表，然后调用addAll添加值，保证对象不会改变
     */
    private void initDiary(){

        SharedPreferences preferences=getSharedPreferences("DATA",MODE_PRIVATE);
        //在初次使用应用时，向数据库导入四篇介绍性文章
        if(preferences.getBoolean("FIRST",true)){
            LitePal.getDatabase();  //创建数据库
            Gson gson=new Gson();
            String jsonData=getJson(MainActivity.this,"defaultDiary.json");
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



        if(diaryList!=null) {
            diaryList.clear();
            diaryList.addAll(LitePal.order("date desc,time desc").find(Diary.class));
        }else {
            diaryList=LitePal.order("date desc,time desc").find(Diary.class);
        }


    }

    //读取json文件
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

    //刷新日记展示内容
    private void refreshDiary(){
        initDiary();  //初始化日记列表
        adapter.notifyDataSetChanged();  //提示适配器检查数据，数据已改变
        swipeRefreshLayout.setRefreshing(false);  //关闭刷新状态
    }




    //对菜单的响应事件
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            //修改HomeAsUp按钮的功能
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            //搜索功能
            case R.id.search:

                break;
            //时钟功能
            case R.id.clock:

                break;
            //写日记功能
            case R.id.write_diary_menu:
                Intent intent=new Intent(MainActivity.this,EditDiaryActivity.class);
                startActivity(intent);
                finish();  //这里结束当前活动，方便在保存日记回到该页面时重新调用onCreate方法进而刷新日记内容
                break;
            //简单布局
            case R.id.simple_adapter_menu:
                SharedPreferences.Editor editor2=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                editor2.putInt("TYPE",SIMPLE_TYPE);
                editor2.apply();
                Intent intent2=new Intent(MainActivity.this,MainActivity.class);
                startActivity(intent2);
                finish();
                break;
            //卡片布局
            case R.id.adapter_menu:
                SharedPreferences.Editor editor=getSharedPreferences("TYPE",MODE_PRIVATE).edit();
                editor.putInt("TYPE",GENERAL_TYPE);
                editor.apply();
                Intent intent1=new Intent(MainActivity.this,MainActivity.class);
                startActivity(intent1);
                finish();
                break;
            //设置
            case R.id.setting_menu:

                break;
            //意见功能
            case R.id.opinion_menu:

                break;

            default:
        }
        return true;
    }




}
