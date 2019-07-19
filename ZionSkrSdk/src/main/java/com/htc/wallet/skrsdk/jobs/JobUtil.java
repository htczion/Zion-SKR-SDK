package com.htc.wallet.skrsdk.jobs;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.support.annotation.NonNull;

import java.util.Objects;

public class JobUtil {

    public static boolean isJobExist(@NonNull JobScheduler jobScheduler, int jobId) {
        Objects.requireNonNull(jobScheduler, "jobScheduler is null");
        final JobInfo jobInfo = jobScheduler.getPendingJob(jobId);
        return jobInfo != null;
    }
}
