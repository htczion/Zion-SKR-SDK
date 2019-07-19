package com.htc.wallet.skrsdk.jobs;

import java.util.Arrays;
import java.util.List;

public final class JobIdManager {

    public static final int JOB_ID_SKR_HEALTH_CHECK = 33001;
    public static final int JOB_ID_SKR_HEALTH_REPORT = 32970;
    public static final int JOB_ID_SKR_USER_MANUAL_CHECK = 32971;
    public static final int JOB_ID_SKR_CHECK_AUTO_BACKUP = 32972;
    public static final int JOB_ID_SKR_CHECK_BACKUP_VERSION = 32973;
    public static final int JOB_ID_SKR_UPLOAD_TRUST_CONTACTS = 33501;

    // Deprecated periodical job's id
    // JOB_ID_SKR_HEALTH_CHECK, 32968, JobIdManager.getJobId(1, 200)
    // JOB_ID_SKR_HEALTH_REPORT, 65636, JobIdManager.getJobId(2, 100)
    // JOB_ID_SKR_HEALTH_CHECK, 32969, Change period from 10 days to 3 days
    public static final List<Integer> DEPRECATED_JOB_IDS = Arrays.asList(32968, 65636, 32969);


    // Working with Multiple JobServices
    // https://android-developers.googleblog.com/2017/10/working-with-multiple-jobservices.html

    /*
    private static final int JOB_TYPE_SHIFTS = 15;
    private static int getJobId(int jobType, int objectId) {
        if (0 < objectId && objectId < (1 << JOB_TYPE_SHIFTS)) {
            return (jobType << JOB_TYPE_SHIFTS) + objectId;
        } else {
            String err = String.format("objectId %s must be between %s and %s",
                    objectId, 0, (1 << JOB_TYPE_SHIFTS));
            throw new IllegalArgumentException(err);
        }
    }*/

    private JobIdManager() {
    }
}
