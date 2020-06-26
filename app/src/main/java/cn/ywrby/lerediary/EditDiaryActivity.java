package cn.ywrby.lerediary;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import cn.ywrby.lerediary.db.Diary;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.widemouth.library.toolitem.*;
import com.widemouth.library.wmview.WMEditText;
import com.widemouth.library.wmview.WMToolContainer;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditDiaryActivity extends AppCompatActivity {

    /* *******************************************************************************************
     *
     * 声明主要变量
     *
     * *******************************************************************************************/

    private TextView edit_cal;  //日期选择
    private TextView edit_time;  //时间选择
    private EditText edit_title;  //标题
    private ImageView edit_mood;  //天气选择
    private ImageView edit_album;  //打开相册，插入图片
    private ImageView edit_voice;  //打开语音识别控制栏
    private ImageView edit_text;  //打开富文本编辑器控制栏
    private ImageView edit_clear;  //弹出清空文本提示框
    private ImageButton edit_speech;  //语音控制开启/结束按钮


    // 使用 开源 富文本编辑器  https://github.com/widemouth-dz/wmrichtexteditor
    private WMEditText edit_content;  //富文本编辑器
    private WMToolContainer toolContainer;  //富文本编辑器控制栏
    private final WMToolItem toolBold = new WMToolBold();  //加粗
    private final WMToolItem toolItalic = new WMToolItalic();  //斜体
    private final WMToolItem toolUnderline = new WMToolUnderline();  //下划线
    private final WMToolItem toolStrikethrough = new WMToolStrikethrough();  //删除线
    private final WMToolItem toolTextColor = new WMToolTextColor();  //文本颜色
    private final WMToolItem toolBackgroundColor = new WMToolBackgroundColor();  //背景颜色
    private final WMToolItem toolTextSize = new WMToolTextSize();  //字体大小
    private final WMToolItem toolListNumber = new WMToolListNumber();  //有序列表
    private final WMToolItem toolListBullet = new WMToolListBullet();  //无序列表
    private final WMToolItem toolAlignment = new WMToolAlignment();  //对齐方式
    private final WMToolItem toolQuote = new WMToolQuote();  //引用
    private final WMToolItem toolListClickToSwitch = new WMToolListClickToSwitch();
    private final WMToolItem toolSplitLine = new WMToolSplitLine();  //分隔线


    private String diaryDate;  //日记的日期
    private String diaryTime;  //日记的时间
    private String diaryCover;  //日记封面路径
    private int diaryWeather;  //代表天气的ID
    private String diaryUuid;  //标识日记的唯一序列，一般在创建时自动赋值，这里定义是为了重新编辑时获取到原序列进行数据库的更新
    private final SimpleDateFormat dateformat = new SimpleDateFormat("yyyy年MM月dd日");  //日期格式化模式
    private final SimpleDateFormat timeformat=new SimpleDateFormat("HH:mm");  //时间格式化模式


    private EventManager asr;  //百度语音识别事件处理器

    private final int OPEN_ALBUM=214;  //设定打开相册的requestCode
    private int flagOfEditSpeech=0;  //语音识别的开始结束键被点击次数
    private boolean hasDefaultDiary=false;  //默认不含有传入的日记内容


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_diary);

        //初始化相关控件
        edit_clear=findViewById(R.id.edit_clear);
        edit_title=findViewById(R.id.edit_title);
        edit_cal=findViewById(R.id.edit_calendar);
        edit_time=findViewById(R.id.edit_time);
        edit_mood=findViewById(R.id.edit_mood);
        edit_album=findViewById(R.id.edit_album);
        edit_voice=findViewById(R.id.edit_voice);
        edit_speech=findViewById(R.id.edit_speech);
        edit_content = findViewById(R.id.edit_content);
        toolContainer = findViewById(R.id.WMToolContainer);
        edit_text=findViewById(R.id.edit_text);

        //设置系统状态栏的颜色，使其更符合整个主题
        setStatusBarColor(this, Color.parseColor("#00BBD4") );
        //设置ToolBar
        final Toolbar toolbar=findViewById(R.id.edit_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();  //这里的ActionBar实际上已经是通过ToolBar实现
        if(actionBar!=null){
            //Toolbar最左侧的按钮被称为HomeAsUp按钮，默认图标是返回箭头，作用就是返回上一个活动
            actionBar.setDisplayHomeAsUpEnabled(true);  //允许显示返回按钮
        }


        Intent intent=getIntent();   //获取intent对象
        Diary defaultDiary = null;  //可能存在的默认日记
        //如果携带了预设的Diary对象，说明此时是重新编辑状态，要做的是更新，而不是创建
        try {
            defaultDiary = intent.getParcelableExtra("diary");  //尝试获取Diary
        }catch (Exception ignored){}
        //为空说明是写新日记，一切按照默认要求填充
        if(defaultDiary==null){
            //获取系统时间
            Calendar calendar = Calendar.getInstance();
            //日记的日期默认设置为系统当前的日期
            diaryDate=String.format("%d年%d月%d日",calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH));
            //日记的默认时间是系统时间
            diaryTime=String.format("%02d:%02d",calendar.get(Calendar.HOUR),calendar.get(Calendar.MINUTE));
            //日记默认的天气是mood2晴天
            diaryWeather=2;
            //使用默认图片
            diaryCover=null;

        }else{
            hasDefaultDiary=true;
            //修改默认的日期，时间，天气设置
            diaryDate=defaultDiary.getDate();
            diaryTime=defaultDiary.getTime();
            diaryWeather=defaultDiary.getWeather();
            //直接向标题和正文写入内容，同时修改天气图标（因为默认的天气图标是在XML文件中就定义好的）
            edit_mood.setImageResource(initWeather(diaryWeather));
            edit_title.setText(defaultDiary.getTitle());
            edit_content.fromHtml(defaultDiary.getContent());
            diaryCover=defaultDiary.getCover();
            Log.d("ywrby1", "onCreate: "+diaryCover);
            diaryUuid=defaultDiary.getUuid();
        }


        /* *******************************************************************************************
         *
         * 操作按钮 设置点击事件
         *
         * *******************************************************************************************/

        //选择日期
        edit_cal.setText(diaryDate);  //格式化日期
        edit_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCalendar();
            }
        });

        //选择时间
        edit_time.setText(diaryTime);  //格式化时间
        edit_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTime();
            }
        });

        //选择天气
        edit_mood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMood();
            }
        });

        //相册内选择图片
        edit_album.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                /*
                这里出现了比较严重的问题，即便动态申请读写权限也不能正常得到图片
                其原因在于从Android Q(即 Android 10)开始，就算应用有申请READ_EXTERNAL_STORAGE 或
                 WRITE_EXTERNAL_STORAGE权限，也只能访问外部存储的私有目录，
                 若是访问了除了私有目录之外的其他外部储存，会抛出FileNotFoundException异常
                 解决方案就是把应用的 targetSdkVersion设置为29以下即可

                 https://blog.csdn.net/qq_43278826/article/details/103088419
                 */

                if(ContextCompat.checkSelfPermission(EditDiaryActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=
                        PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(EditDiaryActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=
                                PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(EditDiaryActivity.this,new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
                    },1);
                }else {
                    showCover();
                }


            }
        });



        //弹出语音识别栏
        edit_speech.setVisibility(View.GONE);  //设为不可见并去除所在分配空间
        edit_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断控件当前的显示状态并作出修改
                if(edit_speech.getVisibility()==View.GONE) {
                    //动态请求语音识别的权限
                    initPermissionOfSpeechRecognition();
                    //显示语音识别控制区域
                    edit_speech.setVisibility(View.VISIBLE);  //设为可见
                    //设置按钮显示键盘，表示再次点击切换回软键盘
                    edit_voice.setImageResource(R.drawable.edit_keyboard);

                    //防止语音识别栏和文本控制栏同时升起，所以先判断对方状态
                    if(edit_text.getVisibility()==View.VISIBLE){
                        toolContainer.setVisibility(View.GONE);
                    }
                }else{
                    //隐藏语音识别控制区域
                    edit_speech.setVisibility(View.GONE);
                    //将图标切换回语音图标
                    edit_voice.setImageResource(R.drawable.edit_voice);
                }
            }
        });

        asr = EventManagerFactory.create(EditDiaryActivity.this,"asr");//注册自己的输出事件类
        asr.registerListener(new EventListener() {
            @Override
            public void onEvent(String name, String params, byte[] data, int offset, int length) {
                String resultTxt = null;
                if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)){//识别结果参数
                    if (params.contains("\"final_result\"")){//语义结果值
                        try {
                            JSONObject json = new JSONObject(params);
                            String result = json.getString("best_result");//取得key的识别结果
                            resultTxt = result;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (resultTxt != null){
                    resultTxt += "\n";
                    edit_content.append(resultTxt);
                }
            }
        });// 调用 EventListener 中 onEvent方法

        //对语音识别栏中的点击事件进行处理

        edit_speech.setOnClickListener(new NoDoubleClickListenerToSpeech() {
            @Override
            protected void onNoDoubleClick(View v) {
                //根据被点击的次数决定录音状态
                if(flagOfEditSpeech%2==0){
                    Toast.makeText(EditDiaryActivity.this,"正在语音识别(仅支持短语音识别20s)",Toast.LENGTH_SHORT).show();
                    speechRecognitionStart();
                    edit_speech.setImageResource(R.drawable.edit_end);
                    flagOfEditSpeech+=1;
                }else {
                    Toast.makeText(EditDiaryActivity.this,"语音识别结束",Toast.LENGTH_SHORT).show();
                    speechRecognitionStop();
                    edit_speech.setImageResource(R.drawable.edit_voice);
                    flagOfEditSpeech+=1;
                }
            }
        });




        //弹出富文本控制栏
        toolContainer.setVisibility(View.GONE);  //起初将富文本控制栏设为不可见
        initContainerTool();  //加载控制栏中的各个控件，并将控制栏与富文本编辑器绑定
        edit_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(toolContainer.getVisibility()==View.GONE){
                    toolContainer.setVisibility(View.VISIBLE);
                    if(edit_voice.getVisibility()==View.VISIBLE){
                        //隐藏语音识别控制区域
                        edit_speech.setVisibility(View.GONE);
                        //将图标切换回语音图标
                        edit_voice.setImageResource(R.drawable.edit_voice);
                    }
                }else {
                    toolContainer.setVisibility(View.GONE);
                }
            }
        });


        //清空文本内容
        edit_clear.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                showClear();
            }
        });



    }

    //弹出日期选择器
    public void showCalendar(){
        //获取系统时间
        Calendar calendar = Calendar.getInstance();
        //创建一个日期选择对话框，第一个参数是展示的context，第二个是所需的监听器，第三四五个分别是年月日
        DatePickerDialog datePickerDialog = new DatePickerDialog(EditDiaryActivity.this, new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear+=1;
                //修改日记的日期并在日记的日期处显示
                diaryDate=year + "年" + monthOfYear + "月" + dayOfMonth+ "日";
                edit_cal.setText(diaryDate);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();

    }

    //弹出时间选择器
    public void showTime(){
        //获取系统时间
        Calendar calendar = Calendar.getInstance();
        //创建一个时间选择对话框，第一个参数是展示的context，第二个是所需的监听器，第三四个分别是小时和分钟，最后一个是是否为24小时制
        TimePickerDialog timePickerDialog = new TimePickerDialog(EditDiaryActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String hourStr=String.format("%02d",hourOfDay);
                String minuteStr=String.format("%02d",minute);
                diaryTime=hourStr+":"+minuteStr;
                edit_time.setText(diaryTime);
            }
        },calendar.get(Calendar.HOUR),calendar.get(Calendar.MINUTE),false);
        timePickerDialog.show();

    }

    //弹出天气选择框
    public void showMood(){
        View moodDialogView=View.inflate(EditDiaryActivity.this,R.layout.mood_list,null);
        //创建自定义选择框
        final AlertDialog.Builder moodDialogBuilder = new AlertDialog.Builder(EditDiaryActivity.this);
        moodDialogBuilder
                .setView(moodDialogView)
                .create();
        AlertDialog moodDialog=moodDialogBuilder.show();
        Window moodWin=moodDialog.getWindow();
        moodWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = moodWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        moodWin.setAttributes(params);
        //传入两个与天气图标相关联的列表，帮助他们批量注册响应事件
        int[] idList={R.id.mood_1,R.id.mood_2,R.id.mood_3,R.id.mood_4,
                R.id.mood_5,R.id.mood_6,R.id.mood_7,R.id.mood_8};
        int[] iconList={R.drawable.mood1,R.drawable.mood2,R.drawable.mood3,R.drawable.mood4,
                R.drawable.mood5,R.drawable.mood6,R.drawable.mood7,R.drawable.mood8};
        moodOnClick(idList,iconList,moodDialog,moodWin);  //设置响应事件

    }
    //批量处理在天气弹出框内选择天气后的响应事件
    private void moodOnClick(final int[] idList, final int[] iconList, final AlertDialog dialog, Window win){

        for(int i=0;i<idList.length;i++) {
            final int finalI = i;
            win.findViewById(idList[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    edit_mood.setImageResource(iconList[finalI]);  //修改天气图标
                    diaryWeather= finalI+1;  //修改日记的天气参数
                    Log.d("YWRBY1", "onClick: "+diaryWeather);
                    dialog.dismiss();  //关闭弹出框
                }
            });
        }
    }

    //展示相册选择照片
    /*
    getBitmapMime()
    insertPhotoToEditText()
    displayImage()
    方法参考自：https://github.com/yinyoupoet/FLAGS/tree/master
     */
    private void showAlbum(){
        Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);
        getAlbum.setType("image/*");
        startActivityForResult(getAlbum,OPEN_ALBUM);
    }

    //通过图片Uri解析图片真实路径，针对Android4.4以上版本解析数据
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data){
        String imagePath=null;
        Uri uri=data.getData();
        Log.d("YWRBY1", "handleImageOnKitKat: "+uri.toString());
        if(DocumentsContract.isDocumentUri(this,uri)){
            String docID=DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id=docID.split(":")[1];
                String selection=MediaStore.Images.Media._ID+"="+id;
                imagePath=getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri= ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docID));
                imagePath=getImagePath(contentUri,null);
            }
        }else if("content".equalsIgnoreCase(uri.getScheme())){
            imagePath=getImagePath(uri,null);
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            imagePath=uri.getPath();
        }
        //创建新的文件名和文件路径，避免用户插入本地图片然后删除，导致路径报错

        String newFileName="lere_"+System.currentTimeMillis()+".jpg";
        File newFile=new File(getExternalFilesDir("photo"),newFileName);
        diaryCover=getExternalFilesDir("photo")+"/"+newFileName;
        if(!copyFile(imagePath,diaryCover)){
            diaryCover=null;
        }

    }

    //通过图片Uri解析图片真实路径，针对Android4.4之前的版本
    private void handleImageBeforeKitKat(Intent data){
        Uri uri=data.getData();
        String imagePath=getImagePath(uri,null);
        //创建新的文件名和文件路径，避免用户插入本地图片然后删除，导致路径报错
        String newFileName="lere_"+System.currentTimeMillis()+".jpg";
        File newFile=new File(getExternalFilesDir("photo"),newFileName);
        diaryCover=getExternalFilesDir("photo")+"/"+newFileName;
        if(!copyFile(imagePath,diaryCover)){
            diaryCover=null;
        }
    }

    //复制单个文件到应用file文件夹中，避免因为用户删除手机图片导致日记文件损坏
    public boolean copyFile(String oldPathName, String newPathName) {
        try {
            File oldFile = new File(oldPathName);
            FileInputStream fileInputStream = new FileInputStream(oldPathName);
            FileOutputStream fileOutputStream = new FileOutputStream(newPathName);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fileInputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //获取图像路径
    private String getImagePath(Uri uri,String selection){
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


    private void showCover(){
        View coverDialogView=View.inflate(EditDiaryActivity.this,R.layout.cover_item,null);
        //创建自定义选择框
        final AlertDialog.Builder coverDialogBuilder = new AlertDialog.Builder(EditDiaryActivity.this);
        coverDialogBuilder
                .setView(coverDialogView)
                .create();
        final AlertDialog coverDialog=coverDialogBuilder.show();
        Window coverWin=coverDialog.getWindow();
        ImageView cover=coverWin.findViewById(R.id.cover_cover);
        //设置封面
        if(diaryCover!=null) {
            Glide.with(coverDialogView)
                    .load(diaryCover)
                    .override(800, 400)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(20)))//设置圆角和裁切图片
                    .into(cover);
        }

        coverWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = coverWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        coverWin.setAttributes(params);
        //设置响应事件
        Button cover_select=coverWin.findViewById(R.id.cover_select);

        cover_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlbum();
                coverDialog.dismiss();
            }
        });
        Button cover_delete=coverWin.findViewById(R.id.cover_delete);
        cover_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diaryCover=null;
                coverDialog.dismiss();
            }
        });
    }


    //语音识别开始
    private void speechRecognitionStart(){
        Map<String,Object> params = new LinkedHashMap<>();//传递Map<String,Object>的参数，会将Map自动序列化为json
        String event = null;
        event = SpeechConstant.ASR_START;
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME,false);//回调当前音量
        String json = null;
        json = new JSONObject(params).toString();//demo用json数据来做数据交换的方式
        asr.send(event, json, null, 0, 0);// 初始化EventManager对象,这个实例只能创建一次，就是我们上方创建的asr，此处开始传入
    }
    //语音识别结束
    private void speechRecognitionStop(){
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);//此处停止
    }
    //动态请求语音识别所需的相关权限
    private void initPermissionOfSpeechRecognition() {
        String[] permissions = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.

            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }


    //将所有文本控件载入控制栏并绑定富文本编辑器
    private void initContainerTool(){
        toolContainer.addToolItem(toolTextColor);
        toolContainer.addToolItem(toolBackgroundColor);
        toolContainer.addToolItem(toolTextSize);
        toolContainer.addToolItem(toolBold);
        toolContainer.addToolItem(toolItalic);
        toolContainer.addToolItem(toolUnderline);
        toolContainer.addToolItem(toolStrikethrough);
        toolContainer.addToolItem(toolListNumber);
        toolContainer.addToolItem(toolListBullet);
        toolContainer.addToolItem(toolAlignment);
        toolContainer.addToolItem(toolQuote);
        toolContainer.addToolItem(toolListClickToSwitch);
        toolContainer.addToolItem(toolSplitLine);
        edit_content.setupWithToolContainer(toolContainer);
    }

    //弹出正文清空提示框
    private void showClear(){
        View clearDialogView=View.inflate(EditDiaryActivity.this,R.layout.clear_edit_text,null);
        //创建自定义选择框
        final AlertDialog.Builder clearDialogBuilder = new AlertDialog.Builder(EditDiaryActivity.this);
        clearDialogBuilder
                .setView(clearDialogView)
                .create();
        final AlertDialog clearDialog=clearDialogBuilder.show();
        Window clearWin=clearDialog.getWindow();
        clearWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = clearWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        clearWin.setAttributes(params);
        //设置响应事件
        Button clear_true=clearWin.findViewById(R.id.clear_true);

        clear_true.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_content.setText("");
                clearDialog.dismiss();
            }
        });
        Button clear_cancel=clearWin.findViewById(R.id.clear_cancel);
        clear_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearDialog.dismiss();
            }
        });

    }

    //弹出保存内容提示框
    private void showSave(){
        View saveDialogView=View.inflate(EditDiaryActivity.this,R.layout.save_edit_text,null);
        //创建自定义选择框
        final AlertDialog.Builder saveDialogBuilder = new AlertDialog.Builder(EditDiaryActivity.this);
        saveDialogBuilder
                .setView(saveDialogView)
                .create();
        final AlertDialog saveDialog=saveDialogBuilder.show();
        Window saveWin=saveDialog.getWindow();
        saveWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = saveWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        saveWin.setAttributes(params);
        //设置响应事件
        Button save_true=saveWin.findViewById(R.id.save_true);

        save_true.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result=saveDiary();
                if(result){
                    //成功保存，可以关闭页面了
                    saveDialog.dismiss();
                    //跳转回主页
                    Intent intent=new Intent(EditDiaryActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    //保存失败，提醒用户标题和内容不能全为空
                    Toast.makeText(EditDiaryActivity.this,"标题和日记内容不能全为空哦",Toast.LENGTH_SHORT).show();
                    saveDialog.dismiss();
                }
            }
        });
        Button save_cancel=saveWin.findViewById(R.id.save_cancel);
        save_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDialog.dismiss();
            }
        });
    }

    //保存日记内容
    private boolean saveDiary(){
        Diary diary;
        if(!hasDefaultDiary) {
            //初次保存
            diary = new Diary();
        }else {
            //再次编辑
            List<Diary> diaries=LitePal.where("uuid=?",diaryUuid).find(Diary.class);
            diary= diaries.get(0);
        }
        diary.setDate(diaryDate);
        diary.setTime(diaryTime);
        diary.setWeather(diaryWeather);
        diary.setCover(diaryCover);
        EditText title = findViewById(R.id.edit_title);
        WMEditText content = findViewById(R.id.edit_content);
        String titleText = title.getText().toString();
        String contentText = content.getHtml();
        //提示用户标题和正文内容不能都为空
        if (titleText.equals("") && contentText.equals(new WMEditText(EditDiaryActivity.this).getHtml())) {
            return false;
        } else {
            diary.setTitle(titleText);
            diary.setContent(contentText);
            diary.save();
        }
        return true;

    }

    //专门为了限制语音识别开始/结束点击按钮的速度，防止短时间快速点击按钮造成报错
    //其他的按钮限制采用NoDoubleClickListener类，并且不会弹出提示性语句
    public abstract class NoDoubleClickListenerToSpeech implements View.OnClickListener {

        public static final int MIN_CLICK_DELAY_TIME = 1000;//这里设置不能超过多长时间
        private long lastClickTime = 0;

        protected abstract void onNoDoubleClick(View v);


        @Override
        public void onClick(View v) {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                lastClickTime = currentTime;
                onNoDoubleClick(v);
            }else{
                Toast.makeText(v.getContext(), "请勿快速点击", Toast.LENGTH_SHORT).show();
            }
        }



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
        ViewGroup mContentView = window.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null) {
            ViewCompat.setFitsSystemWindows(mChildView, false);
            ViewCompat.requestApplyInsets(mChildView);
        }
    }


    //弹出确认返回提示框
    private void showBack(){
        View backDialogView=View.inflate(EditDiaryActivity.this,R.layout.back_item,null);
        //创建自定义选择框
        final AlertDialog.Builder backDialogBuilder = new AlertDialog.Builder(EditDiaryActivity.this);
        backDialogBuilder
                .setView(backDialogView)
                .create();
        final AlertDialog backDialog=backDialogBuilder.show();
        Window backWin=backDialog.getWindow();
        backWin.setBackgroundDrawable(new BitmapDrawable());  //去除圆弧弹出框多余的空白四角
        WindowManager.LayoutParams params = backWin.getAttributes();
        params.width = 1000;   //设置弹出框的宽度
        backWin.setAttributes(params);
        //设置响应事件
        Button back_delete=backWin.findViewById(R.id.back_delete);
        //舍弃当前日记退出到主页
        back_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backDialog.dismiss();
                Intent intent=new Intent(EditDiaryActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        //保存日记退出
        Button back_save=backWin.findViewById(R.id.back_save);
        back_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result=saveDiary();
                if(result){
                    //成功保存，可以关闭页面了
                    backDialog.dismiss();
                    //跳转回主页
                    Intent intent=new Intent(EditDiaryActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    //保存失败，提醒用户标题和内容不能全为空
                    Toast.makeText(EditDiaryActivity.this,"标题和日记内容不能全为空哦",Toast.LENGTH_SHORT).show();
                    backDialog.dismiss();
                }
            }
        });
        //取消
        Button back_cancel=backWin.findViewById(R.id.back_cancel);
        back_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backDialog.dismiss();
            }
        });

    }

    //获取天气信息
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case OPEN_ALBUM:
                if(resultCode==RESULT_OK) {
                    try {
                        if (Build.VERSION.SDK_INT >= 19) {
                            handleImageOnKitKat(data);
                        } else {
                            handleImageBeforeKitKat(data);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(EditDiaryActivity.this, "图片插入失败", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    showAlbum();
                }else {
                    Toast.makeText(EditDiaryActivity.this,"Failed to open it!",Toast.LENGTH_LONG).show();
                }
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_edit,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                showBack();
                break;
            case R.id.save:
                showSave();
                break;
            default:
        }
        return true;
    }

    //按下返回键之后的操作
    @Override
    public void onBackPressed() {
        showBack();
    }



}
