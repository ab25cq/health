package com.ab25cq.health;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.provider.Settings;
import java.util.*;

public final class StepStore extends SQLiteOpenHelper {
    private static StepStore instance;
    public static synchronized StepStore get(Context c) {
        if (instance == null) instance = new StepStore(c.getApplicationContext());
        return instance;
    }
    private StepStore(Context c) { super(c, "steps.db", null, 1); }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE buckets(id TEXT PRIMARY KEY,start INTEGER,end INTEGER,count INTEGER,version INTEGER,synced INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE sensor(id INTEGER PRIMARY KEY CHECK(id=1),value INTEGER,time INTEGER,boot INTEGER)");
    }
    public void onUpgrade(SQLiteDatabase db,int old,int next) {}
    public synchronized void resetBaseline() { getWritableDatabase().delete("sensor", null, null); }
    public synchronized void sample(Context c,long value,long time) {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            int boot=Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,0);
            long previous=-1, last=time; int lastBoot=-1;
            try(Cursor cur=db.rawQuery("SELECT value,time,boot FROM sensor WHERE id=1",null)) {
                if(cur.moveToFirst()){ previous=cur.getLong(0);last=cur.getLong(1);lastBoot=cur.getInt(2); }
            }
            if(previous>=0 && lastBoot==boot && value>previous && time>last) {
                // Preserve the full interval when the sensor delivers delayed/batched readings.
                String id=null;
                try(Cursor bucket=db.rawQuery("SELECT id FROM buckets WHERE end=? AND start>=? LIMIT 1",new String[]{""+last,""+(time/60000*60000)})){if(bucket.moveToFirst())id=bucket.getString(0);}
                if(id==null) db.execSQL("INSERT INTO buckets(id,start,end,count,version) VALUES(?,?,?,?,1)",new Object[]{UUID.randomUUID().toString(),last,time,value-previous});
                else db.execSQL("UPDATE buckets SET end=?,count=count+?,version=version+1,synced=0 WHERE id=?",new Object[]{time,value-previous,id});
            }
            db.execSQL("INSERT OR REPLACE INTO sensor VALUES(1,?,?,?)",new Object[]{value,time,boot});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public synchronized java.util.List<Row> pending() {
        ArrayList<Row> rows=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,start,end,count,version FROM buckets WHERE synced=0 ORDER BY start LIMIT 200",null)) {
            while(c.moveToNext()) rows.add(new Row(c.getString(0),c.getLong(1),c.getLong(2),c.getLong(3),c.getLong(4)));
        }
        return rows;
    }
    public synchronized void mark(java.util.List<Row> rows) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try { for(Row r:rows) db.execSQL("UPDATE buckets SET synced=1 WHERE id=? AND version=?",new Object[]{r.id,r.version}); db.setTransactionSuccessful(); }
        finally {db.endTransaction();}
    }
    public synchronized long total(long since) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(count),0) FROM buckets WHERE end>?",new String[]{""+since})) { c.moveToFirst();return c.getLong(0); }
    }
    public synchronized long pendingCount() {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(count),0) FROM buckets WHERE synced=0",null)){c.moveToFirst();return c.getLong(0);}
    }
    public record Row(String id,long start,long end,long count,long version) {}
}
