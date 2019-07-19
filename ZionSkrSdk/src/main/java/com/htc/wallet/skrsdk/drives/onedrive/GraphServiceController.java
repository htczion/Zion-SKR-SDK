package com.htc.wallet.skrsdk.drives.onedrive;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.IDriveSearchCollectionPage;
import com.microsoft.graph.extensions.IGraphServiceClient;

import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;

public class GraphServiceController {
    private static final String TAG = "GraphServiceController";
    private final IGraphServiceClient mGraphServiceClient;

    public GraphServiceController() {
        mGraphServiceClient = GraphServiceClientManager.getInstance().getGraphServiceClient();
    }

    private void uploadFile(
            @NonNull final byte[] file, @NonNull final ICallback<DriveItem> callback) {
        if (file == null || file.length == 0) {
            LogUtil.logError(TAG, "uploadFile(), file is null");
            return;
        }
        if (callback == null) {
            LogUtil.logError(TAG, "uploadFile(), callback is null");
            return;
        }
        try {
            mGraphServiceClient
                    .getMe()
                    .getDrive()
                    .getRoot()
                    .getContent()
                    .buildRequest()
                    .put(file, callback);
        } catch (Exception e) {
            LogUtil.logError(TAG, "uploadFile(), error = " + e);
        }
    }

    public void uploadFileById(
            @NonNull final String itemId,
            @NonNull final byte[] file,
            final String fileName,
            @NonNull final ICallback<DriveItem> callback) {
        if (file == null || file.length == 0) {
            LogUtil.logError(TAG, "uploadFileById(), file is null");
            return;
        }

        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logError(TAG, "uploadFileById(), fileName is null");
            return;
        }
        if (callback == null) {
            LogUtil.logError(TAG, "uploadFileById(), callback is null");
            return;
        }
        if (TextUtils.isEmpty(itemId)) {
            uploadFile(file, callback);
            return;
        }
        mGraphServiceClient
                .getMe()
                .getDrive()
                .getItems(itemId)
                .getItemWithPath(fileName)
                .getContent()
                .buildRequest()
                .put(file, callback);
    }

    public void getFile(
            @NonNull final String fileName,
            @NonNull final ICallback<IDriveSearchCollectionPage> callback) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logError(TAG, "getFile(), fileName is null");
            return;
        }
        if (callback == null) {
            LogUtil.logError(TAG, "getFile(), callback is null");
            return;
        }
        mGraphServiceClient.getMe().getDrive().getSearch(fileName).buildRequest().get(callback);
    }

    public void getFileContent(
            @NonNull final String fileId, @NonNull final ICallback<InputStream> callback) {
        if (fileId == null || TextUtils.isEmpty(fileId)) {
            LogUtil.logDebug(TAG, "getFileContent(), fileId is null");
            return;
        }
        if (callback == null) {
            LogUtil.logDebug(TAG, "getFileContent(), callback is null");
            return;
        }
        mGraphServiceClient
                .getMe()
                .getDrive()
                .getItems(fileId)
                .getContent()
                .buildRequest()
                .get(callback);
    }

    // OneDrive sdk doesn't provide apis about appFolder.
    // Need to implement it
    public void getAppFolder(final DriveCallback<retrofit2.Response<AppFolderResponse>> callback) {
        if (callback == null) {
            LogUtil.logError(TAG, "getAppFolder(), callback is null");
            return;
        }
        OneDriveApiService service = OneDriveManager.getInstance().getApiService();
        Call<AppFolderResponse> call = service.getAppFolder();

        call.enqueue(new Callback<AppFolderResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<AppFolderResponse> call,
                    @NonNull retrofit2.Response<AppFolderResponse> response) {
                callback.onComplete(response);
            }

            @Override
            public void onFailure(@NonNull Call<AppFolderResponse> call, @NonNull Throwable t) {
                LogUtil.logError(TAG, "getAppFolder(), error = " + t);
                callback.onFailure(new Exception(t));
            }
        });
    }

    public void deleteFile(
            @NonNull final String itemId,
            @NonNull final String fileName,
            @NonNull final ICallback<Void> callback) {

        if (callback == null) {
            throw new IllegalArgumentException("deleteFile(), callback is null");
        } else if (TextUtils.isEmpty(itemId)) {
            throw new IllegalArgumentException("deleteFile(), itemId is empty");
        } else if (TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("deleteFile(), fileName is empty");
        }

        mGraphServiceClient
                .getMe()
                .getDrive()
                .getItems(itemId)
                .getItemWithPath(fileName)
                .buildRequest()
                .delete(callback);
    }
}
