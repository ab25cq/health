package com.ab25cq.health;
import android.app.job.*;
import android.content.*;
public final class SyncJob extends JobService {
    public static void schedule(Context c) {
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        if(scheduler.getPendingJob(100)==null) scheduler.schedule(new JobInfo.Builder(100,new ComponentName(c,SyncJob.class)).setPeriodic(15*60*1000L).setPersisted(true).build());
    }
    public boolean onStartJob(JobParameters p){ HealthSync.sync(this,()->jobFinished(p,false));return true; }
    public boolean onStopJob(JobParameters p){return true;}
}
