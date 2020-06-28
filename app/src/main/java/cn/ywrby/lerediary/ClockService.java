package cn.ywrby.lerediary;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Date;

public class ClockService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //启动新线程用于设定通知，保证通知时间尽量准确
        new Thread(new Runnable() {
            @Override
            public void run() {
                //点击通知跳转到应用主界面
                Intent intent1=new Intent(ClockService.this,MainActivity.class);
                PendingIntent pi=PendingIntent.getActivity(ClockService.this,0,intent1,0);

                //通知管理器
                NotificationManager manager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                //高版本需要渠道
                //源自：https://blog.csdn.net/hbfuas/article/details/90112202
                if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                    //只在Android O之上需要渠道，这里的第一个参数要和下面的channelId一样
                    NotificationChannel notificationChannel = new NotificationChannel("12","name",NotificationManager.IMPORTANCE_HIGH);
                    //如果这里用IMPORTANCE_NOENE就需要在系统的设置里面开启渠道，通知才能正常弹出
                    manager.createNotificationChannel(notificationChannel);
                }

                //通知基本设置
                Notification notification=new NotificationCompat.Builder(ClockService.this,"12")
                        .setContentTitle("Lere日记-记录一天的欣喜")
                        .setContentText("该写日记喽，来Lere日记享受自己的小天地吧！")
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.logo)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.logo))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .build();

                //显示通知
                manager.notify(1,notification);

            }
        }).start();

        return super.onStartCommand(intent, flags, startId);

    }
}
