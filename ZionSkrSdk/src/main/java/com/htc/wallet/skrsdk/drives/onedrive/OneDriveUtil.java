package com.htc.wallet.skrsdk.drives.onedrive;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.drives.DriveUtil;
import com.htc.wallet.skrsdk.keyserver.WebApiResponseUtils;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.IDriveSearchCollectionPage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class OneDriveUtil extends DriveUtil {
    private static final String TAG = "OneDriveUtil";
    private final GraphServiceController mServiceController = new GraphServiceController();
    private static final ExecutorService sCachedThreadExecutor = Executors.newCachedThreadPool();

    @Override
    public void saveTrustContacts(
            @Nullable final String fileNamePrefix,
            final String trustContacts,
            @NonNull final DriveCallback<Void> callback) {
        if (TextUtils.isEmpty(fileNamePrefix)) {
            saveMessage(TRUST_CONTACT_FILE_NAME_TXT, trustContacts, callback);
        } else {
            saveMessage(
                    DriveUtil.addFilePrefix(fileNamePrefix, TRUST_CONTACT_FILE_NAME_TXT),
                    trustContacts,
                    callback);
        }
    }

    @Override
    public void saveUUIDHash(String UUID, DriveCallback<Void> callback) {
        saveMessage(UUID_FILE_NAME_TXT, UUID, callback);
    }

    private void saveMessage(
            final String fileName, final String message, final DriveCallback<Void> callback) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logError(TAG, "saveMessage(), fileName is null");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            LogUtil.logError(TAG, "saveMessage(), message is null");
            return;
        }
        if (callback == null) {
            LogUtil.logError(TAG, "saveMessage(), callback is null");
            return;
        }
        DriveUtil.writeFileOnLocal(fileName, message);

        final byte[] fileBytes;
        try {
            fileBytes = getFileBytes(fileName);
            if (fileBytes == null) {
                LogUtil.logError(TAG, "saveMessage(), file bytes is null");
                callback.onFailure(new OneDriveUtilException("saveMessage(), file bytes is null"));
                return;
            }
        } catch (Exception e) {
            callback.onFailure(new OneDriveUtilException(
                    "saveMessage(), getFileBytes() failed, Exception e=" + e));
            return;
        }

        sCachedThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServiceController.getAppFolder(new DriveCallback<Response<AppFolderResponse>>() {
                    @Override
                    public void onComplete(final Response<AppFolderResponse> response) {
                        LogUtil.logInfo(TAG, "getAppFolder(), success");

                        if (response == null) {
                            LogUtil.logError(TAG, "getAppFolder(), response is null");
                            callback.onFailure(
                                    new OneDriveUtilException("getAppFolder(), response is null"));
                            return;
                        }

                        if (!isSuccessCode(response.code())) {
                            LogUtil.logDebug(TAG, "getAppFolder(), response errorBody="
                                    + WebApiResponseUtils.getErrorMsg(response));

                            callback.onFailure(new OneDriveRetryException(
                                    "getAppFolder() failed, response code=" + response.code()));
                            return;
                        }

                        final AppFolderResponse body = response.body();
                        if (body == null) {
                            LogUtil.logError(TAG, "getAppFolder(), response body is null");
                            callback.onFailure(new OneDriveUtilException(
                                    "getAppFolder(), response body is null"));
                            return;
                        }

                        final String id = body.getId();
                        if (TextUtils.isEmpty(id)) {
                            LogUtil.logError(TAG, "getAppFolder(), id is null");
                            callback.onFailure(new OneDriveUtilException(
                                    "getAppFolder(), id is null"));
                            return;
                        }

                        mServiceController.uploadFileById(id, fileBytes, fileName,
                                new ICallback<DriveItem>() {
                                    @Override
                                    public void success(final DriveItem driveItem) {
                                        LogUtil.logInfo(TAG,
                                                "saveMessage(), success Modified Time: "
                                                        + driveItem.lastModifiedDateTime.toString());
                                        callback.onComplete(null);
                                    }

                                    @Override
                                    public void failure(ClientException e) {
                                        LogUtil.logError(TAG,
                                                "saveMessage, ClientException e=" + e);
                                        callback.onFailure(e);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LogUtil.logError(TAG, "getAppFolder() failed, Exception e=" + e);
                        callback.onFailure(e);
                    }
                });
            }
        });
    }

    @Override
    public void loadTrustContacts(
            @Nullable final String fileNamePrefix, DriveCallback<String> callback) {
        if (TextUtils.isEmpty(fileNamePrefix)) {
            loadContent(TRUST_CONTACT_FILE_NAME_TXT, callback);
        } else {
            loadContent(
                    DriveUtil.addFilePrefix(fileNamePrefix, TRUST_CONTACT_FILE_NAME_TXT), callback);
        }
    }

    @Override
    public void loadUUIDHash(DriveCallback<String> callback) {
        loadContent(UUID_FILE_NAME_TXT, callback);
    }

    private void loadContent(final String fileName, @NonNull final DriveCallback<String> callback) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logError(TAG, "loadContent(), fileName is null");
            return;
        }
        if (callback == null) {
            LogUtil.logError(TAG, "loadContent(), callback is null");
            return;
        }

        sCachedThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServiceController.getFile(fileName, new ICallback<IDriveSearchCollectionPage>() {
                    @Override
                    public void success(IDriveSearchCollectionPage collectionPage) {
                        LogUtil.logInfo(TAG, "loadContent(), getFile success");

                        final List<DriveItem> driveItems = collectionPage.getCurrentPage();
                        DriveItem foundItem = null;
                        for (DriveItem item : driveItems) {
                            if (item == null) {
                                continue;
                            }
                            if (fileName.equals(item.name)) {
                                if (foundItem == null) {
                                    foundItem = item;
                                } else if (item.lastModifiedDateTime.compareTo(
                                        foundItem.lastModifiedDateTime) > 0) {
                                    LogUtil.logInfo(TAG, "old file: " + foundItem.name
                                            + " modified time " + foundItem.lastModifiedDateTime
                                            + " new file: " + item.name
                                            + " modified time " + item.lastModifiedDateTime);
                                    foundItem = item;
                                }
                            }
                        }
                        if (foundItem == null) {
                            LogUtil.logError(TAG, "File not found");
                            callback.onFailure(new OneDriveUtilException("File not found"));
                            return;
                        }

                        final String fileId = foundItem.id;
                        if (TextUtils.isEmpty(fileId)) {
                            LogUtil.logError(TAG, "File id is empty");
                            callback.onFailure(new OneDriveUtilException("File id is empty"));
                            return;
                        }

                        LogUtil.logInfo(TAG, "loadContent(), Modified Time: "
                                + foundItem.lastModifiedDateTime.toString());
                        mServiceController.getFileContent(fileId, new ICallback<InputStream>() {
                            @Override
                            public void success(InputStream inputStream) {
                                LogUtil.logInfo(TAG, "loadContent(), getFileContent success");
                                try {
                                    inputStreamToString(inputStream, new StreamCallback() {
                                        @Override
                                        public void getContent(String content) {
                                            if (TextUtils.isEmpty(content)) {
                                                LogUtil.logError(TAG, "File content is empty");
                                                callback.onFailure(new OneDriveUtilException(
                                                        "File content is empty"));
                                                return;
                                            }
                                            callback.onComplete(content);
                                        }
                                    });
                                } catch (IOException e) {
                                    LogUtil.logError(TAG, "getFileContent(), IOException e=" + e);
                                    callback.onFailure(e);
                                }
                            }

                            @Override
                            public void failure(ClientException e) {
                                LogUtil.logError(TAG, "getFileContent(), ClientException e=" + e);
                                callback.onFailure(e);
                            }
                        });
                    }

                    @Override
                    public void failure(ClientException e) {
                        LogUtil.logError(TAG, "getFile, ClientException e=" + e);
                        callback.onFailure(e);
                    }
                });
            }
        });
    }

    @Override
    public void deleteFile(final String fileName, @NonNull final DriveCallback<Void> callback) {
        if (callback == null) {
            LogUtil.logError(TAG, "deleteFile(), callback is null",
                    new IllegalArgumentException("deleteFile(), callback is null"));
            return;
        } else if (TextUtils.isEmpty(fileName)) {
            LogUtil.logError(TAG, "deleteFile(), fileName is empty");
            callback.onFailure(new IllegalArgumentException("deleteFile(), fileName is empty"));
            return;
        }

        // use loadContent() to check file exist
        // due to OneDrive sync mechanism, we still can get file name after file deleted for a while
        loadContent(fileName, new DriveCallback<String>() {
            @Override
            public void onComplete(String s) {
                // s empty still need to delete
                if (s == null) {
                    callback.onFailure(new OneDriveUtilException("File not found"));
                    return;
                }

                // delete file
                sCachedThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        // get app folder first
                        mServiceController.getAppFolder(
                                new DriveCallback<Response<AppFolderResponse>>() {
                                    @Override
                                    public void onComplete(
                                            final Response<AppFolderResponse> response) {
                                        LogUtil.logDebug(TAG, "getAppFolder(), success");

                                        if (response == null) {
                                            LogUtil.logError(TAG,
                                                    "getAppFolder(), response is null");
                                            callback.onFailure(
                                                    new OneDriveUtilException(
                                                            "getAppFolder(), response is null"));
                                            return;
                                        }

                                        if (!isSuccessCode(response.code())) {
                                            LogUtil.logDebug(TAG,
                                                    "getAppFolder() failed, response errorBody="
                                                            + WebApiResponseUtils.getErrorMsg(
                                                            response));

                                            callback.onFailure(new OneDriveRetryException(
                                                    "getAppFolder() failed, response code="
                                                            + response.code()));
                                            return;
                                        }

                                        final AppFolderResponse body = response.body();
                                        if (body == null) {
                                            LogUtil.logError(TAG,
                                                    "getAppFolder(), response body is null");
                                            callback.onFailure(new OneDriveUtilException(
                                                    "getAppFolder(), response body is null"));
                                            return;
                                        }

                                        final String id = body.getId();
                                        if (TextUtils.isEmpty(id)) {
                                            LogUtil.logError(TAG, "getAppFolder(), id is null");
                                            callback.onFailure(new OneDriveUtilException(
                                                    "getAppFolder(), id is null"));
                                            return;
                                        }

                                        // delete file by id and fileName
                                        mServiceController.deleteFile(id, fileName,
                                                new ICallback<Void>() {
                                                    @Override
                                                    public void success(Void aVoid) {
                                                        LogUtil.logDebug(TAG,
                                                                "deleteFile() success");
                                                        callback.onComplete(aVoid);
                                                    }

                                                    @Override
                                                    public void failure(ClientException e) {
                                                        LogUtil.logDebug(TAG,
                                                                "deleteFile() failed, e=" + e);
                                                        callback.onFailure(e);
                                                    }
                                                });

                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        LogUtil.logError(TAG, "getAppFolder() failed, e=" + e);
                                        callback.onFailure(e);
                                    }
                                });
                    }
                });

            }

            @Override
            public void onFailure(Exception e) {
                LogUtil.logDebug(TAG, "loadContent failed, e=" + e);
                callback.onFailure(e);
            }
        });
    }

    private void inputStreamToString(
            @NonNull final InputStream inputStream, @NonNull final StreamCallback callback)
            throws IOException {
        if (inputStream == null) {
            LogUtil.logError(TAG, "inputStreamToString(), inputStream is null");
            return;
        }
        if (callback == null) {
            LogUtil.logError(TAG, "inputStreamToString(), callback is null");
            return;
        }
        sCachedThreadExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        final BufferedReader reader =
                                new BufferedReader(new InputStreamReader(inputStream));
                        final StringBuilder total = new StringBuilder();
                        String string;
                        try {
                            while ((string = reader.readLine()) != null) {
                                total.append(string);
                            }
                        } catch (IOException e) {
                            LogUtil.logError(TAG, "inputStreamToString(), error = " + e);
                            return;
                        }
                        callback.getContent(total.toString());
                    }
                });
    }

    private interface StreamCallback {
        void getContent(String content);
    }

    private boolean isSuccessCode(int code) {
        return code == 200;
    }
}
