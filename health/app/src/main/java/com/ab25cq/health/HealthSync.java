package com.ab25cq.health;

import android.content.*;
import android.content.pm.PackageManager;
import android.health.connect.*;
import android.health.connect.datatypes.*;
import android.os.OutcomeReceiver;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HealthSync {
    public static final String WRITE="android.permission.health.WRITE_STEPS";
    private static final AtomicBoolean busy=new AtomicBoolean();
    public static void sync(Context context,Runnable done) {
        Context c=context.getApplicationContext();
        if(!busy.compareAndSet(false,true)){done.run();return;}
        next(c,done);
    }
    private static void finish(Context c,String message,Runnable done) {
        c.getSharedPreferences("state",0).edit().putString("syncStatus",message).apply();
        busy.set(false);done.run();
    }
    private static void next(Context c,Runnable done) {
        if(c.checkSelfPermission(WRITE)!=PackageManager.PERMISSION_GRANTED){finish(c,"ヘルスコネクトの歩数書き込み権限が必要です",done);return;}
        var rows=StepStore.get(c).pending();
        if(rows.isEmpty()){finish(c,"同期済み",done);return;}
        ArrayList<android.health.connect.datatypes.Record> records=new ArrayList<>();
        for(var row:rows) {
            Instant start=Instant.ofEpochMilli(row.start()),end=Instant.ofEpochMilli(row.end());
            Metadata metadata=new Metadata.Builder().setClientRecordId(row.id()).setClientRecordVersion(row.version())
                .setRecordingMethod(Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED)
                .setDevice(new Device.Builder().setType(Device.DEVICE_TYPE_PHONE).setManufacturer(android.os.Build.MANUFACTURER).setModel(android.os.Build.MODEL).build()).build();
            records.add(new StepsRecord.Builder(metadata,start,end,row.count())
                .setStartZoneOffset(ZoneId.systemDefault().getRules().getOffset(start))
                .setEndZoneOffset(ZoneId.systemDefault().getRules().getOffset(end)).build());
        }
        try {
            HealthConnectManager manager=c.getSystemService(HealthConnectManager.class);
            if(manager==null){finish(c,"ヘルスコネクトを利用できません",done);return;}
            manager.insertRecords(records,c.getMainExecutor(),new OutcomeReceiver<InsertRecordsResponse,HealthConnectException>() {
                public void onResult(InsertRecordsResponse result) {
                    StepStore.get(c).mark(rows);
                    c.getSharedPreferences("state",0).edit().putLong("lastSync",System.currentTimeMillis()).apply();
                    next(c,done);
                }
                public void onError(HealthConnectException e){finish(c,"同期待ち（エラー "+e.getErrorCode()+"）。後で再試行します",done);}
            });
        }catch(Exception e){finish(c,"同期できません。権限とヘルスコネクトを確認してください",done);}
    }
}
