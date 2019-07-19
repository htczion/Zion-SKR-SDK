package com.htc.wallet.skrsdk.applink;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class FirebaseAppLink implements AppLink {
    private static final String TAG = "FirebaseAppLink";
    private final Uri mDeepLink;

    private ShortLinkTaskListener mShortLinkTaskListener;

    private ShortLinkFailureListener mFailureListener;

    public FirebaseAppLink(@NonNull final Uri link) {
        mDeepLink = Objects.requireNonNull(link);
    }

    @Override
    public void createLinkTask(@NonNull final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        if (!NetworkUtil.isNetworkConnected(context)) {
            LogUtil.logDebug(TAG, "createLinkTask fail, no network");
            return;
        }
        final String dynamicLinkDomain = context.getString(R.string.applink_domain);
        String appStoreLink = context.getString(R.string.applink_app_store_link);
        DynamicLink dynamicLink =
                FirebaseDynamicLinks.getInstance()
                        .createDynamicLink()
                        .setLink(mDeepLink)
                        .setDynamicLinkDomain(dynamicLinkDomain)
                        .setAndroidParameters(
                                new DynamicLink.AndroidParameters.Builder(context.getString(
                                        R.string.applink_android_package_name)).build())
                        .setIosParameters(
                                new DynamicLink.IosParameters.Builder(
                                        context.getString(R.string.applink_ios_bundle_id))
                                        .setAppStoreId(
                                                context.getString(R.string.applink_appstore_id))
                                        .setFallbackUrl(Uri.parse(appStoreLink))
                                        .build())
                        .buildDynamicLink();

        FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLongLink(dynamicLink.getUri())
                .buildShortDynamicLink()
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                LogUtil.logError(
                                        TAG, "FirebaseDynamicLinks, onFailure error = " + e);
                                mFailureListener.onFailure(e);
                            }
                        })
                .addOnCompleteListener(
                        new OnCompleteListener<ShortDynamicLink>() {
                            @Override
                            public void onComplete(@NonNull final Task<ShortDynamicLink> task) {
                                Objects.requireNonNull(task);
                                if (task.isSuccessful()) {
                                    setupShortLinkToListener(task);
                                } else {
                                    LogUtil.logError(
                                            TAG,
                                            "FirebaseDynamicLinks, onComplete error = "
                                                    + task.getException());
                                    mFailureListener.onFailure(task.getException());
                                }
                            }
                        });
    }

    private void setupShortLinkToListener(@NonNull final Task<ShortDynamicLink> task) {
        Objects.requireNonNull(task);
        final ShortDynamicLink shortDynamicLink = task.getResult();
        if (shortDynamicLink != null) {
            final Uri shortLink = shortDynamicLink.getShortLink();
            if (mShortLinkTaskListener != null) {
                mShortLinkTaskListener.onShortLinkTaskFinished(shortLink);
            }
        }
    }

    @Override
    public void setShortLinkTaskListener(final ShortLinkTaskListener shortLinkTaskListener) {
        mShortLinkTaskListener = shortLinkTaskListener;
    }

    @Override
    public void setFailureListener(final ShortLinkFailureListener failureListener) {
        mFailureListener = failureListener;
    }
}
