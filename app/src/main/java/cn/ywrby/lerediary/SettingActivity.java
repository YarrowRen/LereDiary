package cn.ywrby.lerediary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
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

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {


    private Context mContext;

    private final int SIMPLE_TYPE=1;
    private final int GENERAL_TYPE=2;

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
                break;
            case R.id.setting_input:
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
}
