package com.htc.wallet.skrsdk.drives.onedrive;

import retrofit2.Call;
import retrofit2.http.GET;

// OneDrive sdk doesn't provide all api (e.g app folder)
// If need functions which sdk doesn't provide, can implement they in this interface
// OneDrive api reference
// https://docs.microsoft.com/en-us/onedrive/developer/rest-api/?view=odsp-graph-online
public interface OneDriveApiService {
    @GET("special/approot")
    Call<AppFolderResponse> getAppFolder();
}
