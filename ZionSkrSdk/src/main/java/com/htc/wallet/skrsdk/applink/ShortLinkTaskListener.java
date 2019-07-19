package com.htc.wallet.skrsdk.applink;

import android.net.Uri;
import android.support.annotation.NonNull;

public interface ShortLinkTaskListener {
    void onShortLinkTaskFinished(final @NonNull Uri shortLink);
}
