package com.ab25cq.health;
import android.content.*;
import android.content.pm.PackageManager;
public final class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context c,Intent i){
        if(!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))return;
        SyncJob.schedule(c);
        if(c.getSharedPreferences("state",0).getBoolean("running",false) && c.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED){
            StepStore.get(c).resetBaseline();
            try{c.startForegroundService(new Intent(c,StepService.class));}catch(RuntimeException e){c.getSharedPreferences("state",0).edit().putString("error","再起動後の測定再開にはアプリで開始を押してください").apply();}
        }
    }
}
