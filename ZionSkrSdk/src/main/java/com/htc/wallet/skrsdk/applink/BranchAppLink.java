package com.htc.wallet.skrsdk.applink;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;


public class BranchAppLink implements AppLink {
    private static final String TAG = "BranchAppLink";

    private final LinkProperties mLinkProperties = new LinkProperties();
    private ShortLinkTaskListener mShortLinkTaskListener;
    private ShortLinkFailureListener mFailureListener;

    public BranchAppLink(@NonNull final Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            mLinkProperties.addControlParameter(key, value);
        }
    }

    @Override
    public void createLinkTask(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        if (!NetworkUtil.isNetworkConnected(context)) {
            LogUtil.logDebug(TAG, "createLinkTask fail, no network");
            return;
        }
        BranchUniversalObject buo = new BranchUniversalObject();
        buo.generateShortUrl(
                context,
                mLinkProperties,
                new Branch.BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        if (error == null) {
                            if (mShortLinkTaskListener != null) {
                                mShortLinkTaskListener.onShortLinkTaskFinished(Uri.parse(url));
                            } else {
                                LogUtil.logDebug(TAG, "shortLinkTaskListener is null");
                            }
                        } else {
                            if (mFailureListener != null) {
                                mFailureListener.onFailure(new BranchException(error.toString()));
                            } else {
                                LogUtil.logDebug(TAG, "failureListener is null");
                            }
                        }
                    }
                });
    }

    @Override
    public void setShortLinkTaskListener(ShortLinkTaskListener shortLinkTaskListener) {
        mShortLinkTaskListener = shortLinkTaskListener;
    }

    @Override
    public void setFailureListener(ShortLinkFailureListener failureListener) {
        mFailureListener = failureListener;
    }
}
