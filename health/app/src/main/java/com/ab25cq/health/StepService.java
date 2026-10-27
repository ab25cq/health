package com.ab25cq.health;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.hardware.*;
import android.os.*;
import android.provider.Settings;

public final class StepService extends Service implements SensorEventListener {
    private SensorManager sensors;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long lastNotification;
    private final Runnable sync=new Runnable(){public void run(){HealthSync.sync(StepService.this,()->{});handler.postDelayed(this,15*60*1000L);}};
    public static boolean alive;
    public void onCreate(){super.onCreate();sensors=getSystemService(SensorManager.class);getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("steps","歩数測定",NotificationManager.IMPORTANCE_LOW));}
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,StepService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
        long today=java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new Notification.Builder(this,"steps").setSmallIcon(com.ab25cq.health.R.drawable.ic_steps).setContentTitle("歩数を測定しています")
            .setContentText("今日 "+StepStore.get(this).total(today)+" 歩 ・ 約15分ごとに同期").setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"停止",stop).build()).build();
    }
    public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && "STOP".equals(intent.getAction())){
            getSharedPreferences("state",0).edit().putBoolean("running",false).apply();
            sensors.unregisterListener(this);StepStore.get(this).resetBaseline();HealthSync.sync(this,()->{});stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;
        }
        if(checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
        startForeground(1,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        Sensor sensor=sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(sensor==null || !sensors.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL,0)) {
            getSharedPreferences("state",0).edit().putBoolean("running",false).putString("error","歩数センサーを利用できません").apply();stopSelf();return START_NOT_STICKY;
        }
        alive=true;getSharedPreferences("state",0).edit().putBoolean("running",true).remove("error").apply();
        SyncJob.schedule(this);handler.removeCallbacks(sync);handler.post(sync);return START_STICKY;
    }
    public void onSensorChanged(SensorEvent e){
        long time=System.currentTimeMillis()-(SystemClock.elapsedRealtimeNanos()-e.timestamp)/1000000L;
        StepStore.get(this).sample(this,(long)e.values[0],time);
        if(SystemClock.elapsedRealtime()-lastNotification>10000){getSystemService(NotificationManager.class).notify(1,notification());lastNotification=SystemClock.elapsedRealtime();}
    }
    public void onAccuracyChanged(Sensor s,int a){}
    public void onDestroy(){alive=false;sensors.unregisterListener(this);handler.removeCallbacksAndMessages(null);super.onDestroy();}
    public IBinder onBind(Intent i){return null;}
}
