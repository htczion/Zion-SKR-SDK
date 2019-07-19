package com.htc.wallet.skrsdk.drives.googledrive;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.drives.DriveUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveUtil extends DriveUtil {
    private static final String TAG = "GoogleDriveUtil";
    private static final String FILE_MIME_TYPE = "text/plain";
    private static final String FILE_STREAM_CHARSET = "UTF-8";
    private static final ExecutorService sCachedThreadExecutor = Executors.newCachedThreadPool();

    private Drive mService = null;

    public GoogleDriveUtil(@NonNull final Context context, final String address) {
        if (context == null) {
            throw new IllegalArgumentException("GoogleDriveUtil(), context is null");
        }

        if (TextUtils.isEmpty(address)) {
            throw new IllegalArgumentException("GoogleDriveUtil(), address is null");
        }
        setupService(context, address);
    }

    private void setupService(@NonNull final Context context, final String address) {
        if (context == null) {
            throw new IllegalArgumentException("GoogleDriveUtil(), context is null");
        }

        if (TextUtils.isEmpty(address)) {
            throw new IllegalArgumentException("setupService(), address is null");
        }
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singletonList(DriveScopes.DRIVE));
        credential.setSelectedAccountName(address);
        mService = getDriveService(credential);
    }

    @Override
    public void saveTrustContacts(
            @Nullable final String fileNamePrefix,
            final String trustContacts,
            final DriveCallback<Void> callback) {
        if (TextUtils.isEmpty(fileNamePrefix)) {
            saveMessage(TRUST_CONTACT_FILE_NAME, trustContacts, callback);
        } else {
            saveMessage(
                    DriveUtil.addFilePrefix(fileNamePrefix, TRUST_CONTACT_FILE_NAME),
                    trustContacts,
                    callback);
        }
    }

    @Override
    public void saveUUIDHash(final String UUID, final DriveCallback<Void> callback) {
        saveMessage(UUID_FILE_NAME, UUID, callback);
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

        sCachedThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final File newFile = new File();
                    newFile.setName(fileName);
                    newFile.setMimeType(FILE_MIME_TYPE);
                    newFile.setModifiedTime(new DateTime(System.currentTimeMillis()));
                    final ByteArrayContent content = new ByteArrayContent(FILE_MIME_TYPE,
                            message.getBytes(Charset.forName(FILE_STREAM_CHARSET)));

                    final String fileId = findFileId(fileName);
                    final File completedFile;
                    if (TextUtils.isEmpty(fileId)) {
                        LogUtil.logDebug(TAG, "insert");
                        newFile.setParents(Collections.singletonList("appDataFolder"));
                        completedFile = mService
                                .files()
                                .create(newFile, content)
                                .setFields("id, modifiedTime")
                                .execute();
                    } else {
                        LogUtil.logDebug(TAG, "update");
                        completedFile = mService
                                .files()
                                .update(fileId, newFile, content)
                                .setFields("id, modifiedTime")
                                .execute();
                    }

                    LogUtil.logInfo(TAG, ChecksumUtil.generateChecksum(fileName)
                            + ", Modified time: " + completedFile.getModifiedTime());
                    callback.onComplete(null);
                } catch (UserRecoverableAuthIOException e) {
                    LogUtil.logError(TAG, "saveMessage(), UserRecoverableAuthIOException e=" + e);
                    callback.onFailure(e);
                } catch (IOException e) {
                    LogUtil.logError(TAG, "saveMessage(), IOException e=" + e);
                    callback.onFailure(e);
                }
            }
        });
    }

    @Override
    public void loadTrustContacts(
            @Nullable final String fileNamePrefix, DriveCallback<String> callback) {
        if (TextUtils.isEmpty(fileNamePrefix)) {
            loadContent(TRUST_CONTACT_FILE_NAME, callback);
        } else {
            loadContent(DriveUtil.addFilePrefix(fileNamePrefix, TRUST_CONTACT_FILE_NAME), callback);
        }
    }

    @Override
    public void loadUUIDHash(DriveCallback<String> callback) {
        loadContent(UUID_FILE_NAME, callback);
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
                try {
                    final String fileId = findFileId(fileName);
                    if (TextUtils.isEmpty(fileId)) {
                        throw new GoogleDriveUtilException("File not found");
                    }

                    final String content = downloadFile(mService, fileId);
                    if (TextUtils.isEmpty(content)) {
                        throw new GoogleDriveUtilException("File content is empty");
                    } else {
                        callback.onComplete(content);
                    }

                } catch (UserRecoverableAuthIOException e) {
                    LogUtil.logError(TAG, "loadContent(), UserRecoverableAuthIOException e=" + e);
                    callback.onFailure(e);
                } catch (IOException e) {
                    LogUtil.logError(TAG, "loadContent(), IOException e=" + e);
                    callback.onFailure(e);
                } catch (GoogleDriveUtilException e) {
                    LogUtil.logDebug(TAG, "loadContent(), GoogleDriveUtilException e=" + e);
                    callback.onFailure(e);
                }
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

        sCachedThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    // find file
                    String fileId = findFileId(fileName);
                    if (TextUtils.isEmpty(fileId)) {
                        callback.onFailure(new GoogleDriveUtilException("File not found"));
                        return;
                    }

                    mService.files()
                            .delete(fileId)
                            .execute();
                    LogUtil.logDebug(TAG, "deleteFile() success");
                    callback.onComplete(null);

                } catch (Exception e) {
                    LogUtil.logDebug(TAG, "deleteFile() failed, e=" + e);
                    callback.onFailure(e);
                }
            }
        });
    }

    private FileList getFiles() throws IOException {
        FileList list = null;
        try {
            list =
                    mService.files()
                            .list()
                            .setFields("files(id, name, modifiedTime)")
                            .setSpaces("appDataFolder")
                            .execute();
        } catch (IOException e) {
            LogUtil.logError(TAG, "getFiles(), error = " + e);
            throw e;
        }
        if (list == null) {
            LogUtil.logWarning(TAG, "getFiles(), files are null");
        }
        return list;
    }

    private File findFile(@NonNull final FileList files, @NonNull final String fileName) {
        if (files == null) {
            LogUtil.logWarning(TAG, "findFile(), files are null");
            return null;
        }
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logWarning(TAG, "findFile(), fileName is null");
            return null;
        }
        File foundFile = null;
        for (File file : files.getFiles()) {
            if (file == null) {
                continue;
            }
            if (fileName.equals(file.getName())) {
                if (foundFile == null) {
                    foundFile = file;
                } else {
                    if (file.getModifiedTime().getValue()
                            > foundFile.getModifiedTime().getValue()) {
                        LogUtil.logInfo(
                                TAG,
                                "old file: "
                                        + foundFile.getName()
                                        + " modified time "
                                        + foundFile.getModifiedTime()
                                        + " new file: "
                                        + file.getName()
                                        + "  modified time "
                                        + file.getModifiedTime());
                        foundFile = file;
                    }
                }
            }
        }
        return foundFile;
    }

    private String findFileId(final String fileName) throws IOException {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logWarning(TAG, "findFileId(), fileName is null");
            return "";
        }

        final FileList files = getFiles();
        if (files == null) {
            LogUtil.logWarning(TAG, "findFileId(), files are null");
            return "";
        }

        final File file = findFile(files, fileName);
        if (file == null) {
            LogUtil.logWarning(
                    TAG,
                    "findFileId(), file : "
                            + ChecksumUtil.generateChecksum(fileName)
                            + " is not found");
            return "";
        }

        LogUtil.logInfo(
                TAG,
                ChecksumUtil.generateChecksum(fileName)
                        + " Modified time: "
                        + file.getModifiedTime());

        return file.getId();
    }

    private String downloadFile(final Drive service, final String fileId) throws IOException {
        if (service == null) {
            LogUtil.logError(TAG, "downloadFile(), service is null");
            return null;
        }
        if (TextUtils.isEmpty(fileId)) {
            LogUtil.logError(TAG, "downloadFile(), fileId is null");
            return null;
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            return outputStream.toString(FILE_STREAM_CHARSET);
        } catch (IOException e) {
            LogUtil.logError(TAG, "downloadFile(), error = " + e);
            throw e;
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(
                AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .build();
    }
}
