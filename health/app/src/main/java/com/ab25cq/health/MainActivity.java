package com.ab25cq.health;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.health.connect.HealthConnectManager;
import android.os.*;
import android.widget.*;
import android.view.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MainActivity extends Activity {
    private TextView count,status,detail;
    private Button start,stop;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,1000);}};
    private boolean startRequested;
    public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(28,24,28,24);
        body.setOnApplyWindowInsetsListener((v,insets)->{var bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(28,bars.top+24,28,bars.bottom+24);return insets;});
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);setContentView(scroll);
        text(body,"ヘルスコネクト対応万歩計",22,Color.rgb(0,95,83));
        text(body,"今日の歩数",18,Color.DKGRAY);
        count=text(body,"0 歩",52,Color.rgb(0,110,95));
        status=text(body,"停止中",20,Color.DKGRAY);
        detail=text(body,"",15,Color.DKGRAY);
        start=button(body,"開始",v->begin());
        stop=button(body,"停止",v->{startService(new Intent(this,StepService.class).setAction("STOP"));render();});
        button(body,"今すぐ同期",v->{SyncJob.schedule(this);HealthSync.sync(this,()->render());});
        button(body,"ヘルスコネクトの権限",v->{try{startActivity(new Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS).putExtra(Intent.EXTRA_PACKAGE_NAME,getPackageName()));}catch(ActivityNotFoundException e){Toast.makeText(this,"ヘルスコネクトを利用できません",Toast.LENGTH_LONG).show();}});
        button(body,"データの取り扱い",v->startActivity(new Intent(this,PrivacyActivity.class)));
        text(body,"開始を押すと通知を表示して、画面を閉じても測定します。約15分ごとと停止時に同期します。端末の省電力制御により同期が遅れることがあります。\n\nスマートフォンを携帯して歩いてください。強制停止中・電源が切れている間の歩数は測定できません。",15,Color.DKGRAY);
        SyncJob.schedule(this);
    }
    private TextView text(LinearLayout b,String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,14,0,14);b.addView(t);return t;}
    private Button button(LinearLayout b,String s,View.OnClickListener l){Button v=new Button(this);v.setText(s);v.setOnClickListener(l);b.addView(v,new LinearLayout.LayoutParams(-1,60*getResources().getDisplayMetrics().densityDpi/160));return v;}
    private void begin(){
        startRequested=true;
        if(getSystemService(android.hardware.SensorManager.class).getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)==null){Toast.makeText(this,"この端末には歩数センサーがありません",Toast.LENGTH_LONG).show();startRequested=false;return;}
        if(checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},1);return;}
        if(checkSelfPermission(HealthSync.WRITE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{HealthSync.WRITE},2);return;}
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},3);return;}
        launch();
    }
    private void launch(){
        startRequested=false;
        if(!StepService.alive)StepStore.get(this).resetBaseline();
        try{startForegroundService(new Intent(this,StepService.class));}catch(RuntimeException e){Toast.makeText(this,"測定を開始できません。権限を確認してください",Toast.LENGTH_LONG).show();}
    }
    public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(!startRequested)return;
        if(request==3){launch();return;}
        if(grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED)begin();
        else {startRequested=false;Toast.makeText(this,"開始には身体活動と歩数の書き込み権限が必要です。権限を許可して再度開始してください",Toast.LENGTH_LONG).show();}
    }
    private void render(){
        if(count==null)return;
        long midnight=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        count.setText(String.format(Locale.JAPAN,"%,d 歩",StepStore.get(this).total(midnight)));
        var p=getSharedPreferences("state",0);
        boolean running=StepService.alive;
        status.setText(running?"● 測定中":"停止中");start.setEnabled(!running);stop.setEnabled(running);
        long last=p.getLong("lastSync",0);
        String when=last==0?"まだ同期していません":DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(last));
        detail.setText(getString(R.string.sync_detail,StepStore.get(this).pendingCount(),when,p.getString("syncStatus",""),p.getString("error","")));
    }
    public void onResume(){super.onResume();handler.post(refresh);}
    public void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
