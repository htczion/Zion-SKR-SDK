package com.htc.wallet.skrsdk.drives.onedrive;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalException;

public interface MSALAuthenticationCallback {
    void onSuccess(AuthenticationResult authenticationResult);

    void onError(MsalException exception);

    void onError(Exception exception);

    void onCancel();
}
