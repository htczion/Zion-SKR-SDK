package com.htc.wallet.skrsdk.drives;

import static com.htc.wallet.skrsdk.crypto.ChecksumUtil.SHA256_HEX_LENGTH;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.applink.NetworkUtil;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.drives.onedrive.AuthenticationManager;
import com.htc.wallet.skrsdk.drives.onedrive.DriveDataUpdateUtil;
import com.htc.wallet.skrsdk.drives.onedrive.MSALAuthenticationCallback;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DriveUtil {
    private static final String TAG = "DriveUtil";

    protected static final String TRUST_CONTACT_FILE_NAME = "trustContact";
    protected static final String UUID_FILE_NAME = "UUIDHash";
    protected static final String TRUST_CONTACT_FILE_NAME_TXT = TRUST_CONTACT_FILE_NAME + ".txt";
    protected static final String UUID_FILE_NAME_TXT = UUID_FILE_NAME + ".txt";

    private static final int PREFIX_LENGTH = 10;

    private static final long TIMEOUT = 60L; // 60s for socket timeout

    // Use (no queue) single thread, because multiple thread check is not necessary
    private static final ThreadPoolExecutor sCheckDeprecatedTrustContactsThread =
            ThreadUtil.newFixedThreadPoolNoQueue(1, "drive-util");

    private static String listToString(@NonNull List<BackupTargetForDrive> list) {
        if (list == null) {
            LogUtil.logWarning(TAG, "listToString(), list is null");
            return "";
        }

        final Gson gson = new Gson();
        return gson.toJson(list);
    }

    public static List<BackupTargetForDrive> stringToList(final String string) {
        if (TextUtils.isEmpty(string)) {
            LogUtil.logWarning(TAG, "stringToList(), string is null");
            return new ArrayList<>();
        }
        final Gson gson = new GsonBuilder().serializeNulls().create();
        final Type backupTargetType = new TypeToken<ArrayList<BackupTargetForDrive>>() {
        }.getType();
        return gson.fromJson(string, backupTargetType);
    }

    // Only for SocialKeyRecoveryChooseDriveAccountActivity flowType: checkUUIDInBackupFlowAtFirst
    public static boolean isUUIDHashDifferentFromDriveAtFirst(
            @NonNull final Context context, @NonNull final String UUIDHash) {
        if (context == null) {
            LogUtil.logWarning(TAG, "isUUIDHashDifferentFromDriveAtFirst(), context is null");
            return false;
        }
        if (TextUtils.isEmpty(UUIDHash)) {
            LogUtil.logWarning(
                    TAG, "isUUIDHashDifferentFromDriveAtFirst(), Drive data loss");
            // For new user, no data is on drive
            return false;
        }
        return isUUIDHashDifferentFromDrive(context, UUIDHash);
    }

    public static boolean isUUIDHashDifferentFromDrive(
            @NonNull final Context context, @NonNull final String UUIDHash) {
        if (context == null) {
            LogUtil.logWarning(TAG, "isUUIDHashDifferentFromDrive(), context is null");
            return false;
        }
        if (TextUtils.isEmpty(UUIDHash)) {
            LogUtil.logWarning(TAG, "isUUIDHashDifferentFromDrive(), Google drive data loss");
            // When google drive data loss, this scenario is same as different UUIDHash
            return true;
        }
        if (UUIDHash.length() != ChecksumUtil.getHexChecksumLength()) {
            // Message which is download from google drive, maybe not complete
            // We need to ignore it.
            LogUtil.logWarning(
                    TAG, "isUUIDHashDifferentFromDrive(), Data from google drive, isn't complete");
            return false;
        }

        final String currentUUIDHash = PhoneUtil.getSKRIDHash(context);
        return !currentUUIDHash.equals(UUIDHash);
    }

    // Only for onedrive service
    public static boolean isUUIDHashDifferentFromLocalFile(@NonNull final Context context) {
        byte[] bytes = getFileBytes(UUID_FILE_NAME_TXT);
        if (bytes == null) {
            return true;
        }
        final String fileUUID = new String(bytes, StandardCharsets.UTF_8);
        final String currentUUIDHash = PhoneUtil.getSKRIDHash(context);
        return !currentUUIDHash.equals(fileUUID);
    }

    public static void deleteUUIDHashLocalFile() {
        deleteFile(UUID_FILE_NAME_TXT);
    }

    public static void deleteTrustContactsLocalFile() {
        deleteFile(TRUST_CONTACT_FILE_NAME_TXT);
    }

    private static void deleteFile(final String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logError(TAG, "deleteFile(), fileName is null");
            return;
        }
        final Context context = ZionSkrSdkManager.getInstance().getAppContext();
        if (context == null) {
            LogUtil.logError(TAG, "deleteFile(), context is null");
            return;
        }
        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) {
            if (file.delete()) {
                LogUtil.logDebug(TAG, "deleteFile(), success");
            } else {
                LogUtil.logDebug(TAG, "deleteFile(), fail");
            }
        }
    }

    public static void writeTrustContactsOnLocal(
            @Nullable final String fileNamePrefix, final String message) {
        if (TextUtils.isEmpty(message)) {
            LogUtil.logError(TAG, "writeTrustContactsOnLocal(), message is null");
            return;
        }
        if (TextUtils.isEmpty(fileNamePrefix)) {
            writeFileOnLocal(TRUST_CONTACT_FILE_NAME_TXT, message);
        } else {
            writeFileOnLocal(addFilePrefix(fileNamePrefix, TRUST_CONTACT_FILE_NAME_TXT), message);
        }
    }

    protected static String addFilePrefix(String prefix, String fileName) {
        if (TextUtils.isEmpty(prefix)) {
            LogUtil.logWarning(TAG, "addFilePrefix(), prefix is null");
        }
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logWarning(TAG, "addFilePrefix(), fileName is null");
        }
        return prefix + "_" + fileName;
    }

    // Get partial UUID (hashed) as file prefix
    @Nullable
    public static String getFilePrefix(String uuidHash) {
        if (uuidHash == null) {
            LogUtil.logError(TAG, "getFilePrefix(), uuidHash is null");
            return null;
        }

        int length = uuidHash.length();
        if (length != SHA256_HEX_LENGTH) {
            LogUtil.logWarning(TAG, "getFilePrefix(), incorrect uuidHash length=" + length);
        }

        if (length < PREFIX_LENGTH) {
            LogUtil.logError(TAG, "getFilePrefix(), incorrect uuidHash length=" + length);
            return uuidHash.toLowerCase();
        }

        return uuidHash.substring(0, PREFIX_LENGTH).toLowerCase();
    }

    public static void writeUUIDHashOnLocal(final String message) {
        if (TextUtils.isEmpty(message)) {
            LogUtil.logError(TAG, "writeUUIDHashOnLocal(), message is null");
            return;
        }
        writeFileOnLocal(UUID_FILE_NAME_TXT, message);
    }

    protected static void writeFileOnLocal(final String fileName, final String message) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logError(TAG, "writeFileOnLocal(), fileName is null");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            LogUtil.logError(TAG, "writeFileOnLocal(), message is null");
            return;
        }
        final Context context = ZionSkrSdkManager.getInstance().getAppContext();
        if (context == null) {
            LogUtil.logError(TAG, "writeFileOnLocal(), context is null");
            return;
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(message.getBytes());
        } catch (Exception e) {
            LogUtil.logError(TAG, "writeFileOnLocal(), error = " + e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LogUtil.logError(TAG, "writeFileOnLocal(), stream close, error = " + e);
                }
            }
        }
    }

    public static String getUUIDHashOnLocal() {
        byte[] bytes = getFileBytes(UUID_FILE_NAME_TXT);
        if (bytes == null || bytes.length == 0) {
            LogUtil.logWarning(TAG, "getUUIDHashOnLocal(), bytes is null");
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String getTrustContactsOnLocal() {
        byte[] bytes = getFileBytes(TRUST_CONTACT_FILE_NAME_TXT);
        if (bytes == null || bytes.length == 0) {
            LogUtil.logWarning(TAG, "getTrustContactsOnLocal(), bytes is null");
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void removeLocalFiles() {
        deleteUUIDHashLocalFile();
        deleteTrustContactsLocalFile();
    }

    protected static byte[] getFileBytes(final String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logWarning(TAG, "getFile(), fileName is null");
            return null;
        }

        File file = getFile(fileName);
        if (file != null) {
            return fileToBytes(file);
        }
        return null;
    }

    private static File getFile(final String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtil.logWarning(TAG, "getFile(), fileName is null");
            return null;
        }
        final Context context = ZionSkrSdkManager.getInstance().getAppContext();
        if (context == null) {
            LogUtil.logError(TAG, "getFile(), context is null");
            return null;
        }

        File file = new File(context.getFilesDir(), fileName);
        if (file.exists() && file.canRead()) {
            return file;
        }
        return null;
    }

    private static byte[] fileToBytes(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            FileInputStream buf = new FileInputStream(file);
            int bytesRead = buf.read(bytes, 0, size);
            if (bytesRead == 0) {
                return null;
            }
            return bytes;
        } catch (IOException e) {
            LogUtil.logError(TAG, "fileToBytes(), error = " + e);
        }
        return bytes;
    }

    public static boolean hasGooglePattern(final String address) {
        if (TextUtils.isEmpty(address)) {
            LogUtil.logWarning(TAG, "hasGooglePattern(), address is null");
            return false;
        }
        final String[] googlePatterns = new String[]{"@gmail", "@google"};
        for (String pattern : googlePatterns) {
            if (address.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public static void putServiceType(@NonNull final Context context, @DriveServiceType int type) {
        if (context == null) {
            LogUtil.logWarning(TAG, "putServiceType(), context is null");
            return;
        }
        SkrSharedPrefs.putSocialKMEmailServiceType(context, String.valueOf(type));
    }

    public static int getServiceType(@NonNull final Context context) {
        if (context == null) {
            LogUtil.logWarning(TAG, "getServiceType(), context is null");
            return DriveServiceType.undefined;
        }
        String type = SkrSharedPrefs.getSocialKMEmailServiceType(context);
        return Integer.parseInt(type);
    }

    @WorkerThread
    public static boolean driveAuth(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        final String address = PhoneUtil.getSKREmail(context);
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "address is null or empty");
            return false;
        }

        if (!NetworkUtil.isNetworkConnected(context)) {
            LogUtil.logInfo(TAG, "Network not available, skip");
            return false;
        }

        @DriveServiceType final int driveServiceType = DriveUtil.getServiceType(context);
        LogUtil.logDebug(TAG, "driveAuth, driveServiceType=" + driveServiceType);

        // Only oneDrive needs auth
        if (driveServiceType == DriveServiceType.oneDrive) {
            if (oneDriveAuthSilent(context)) {
                return true;
            } else {
                LogUtil.logError(TAG, "oneDriveAuth failed");
                return false;
            }
        } else {
            return true;
        }
    }

    @WorkerThread
    private static boolean oneDriveAuthSilent(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        final String address = PhoneUtil.getSKREmail(context);
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "address is null or empty");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean isAuthSuccess = new AtomicBoolean(false);

        AuthenticationManager mgr = AuthenticationManager.getInstance();
        try {
            User loggingUser = null;
            List<User> users = mgr.getPublicClient().getUsers();
            for (User user : users) {
                if (address.equals(user.getDisplayableId())) {
                    loggingUser = user;
                    break;
                }
            }
            if (loggingUser != null) {
                mgr.callAcquireTokenSilent(loggingUser, true, new MSALAuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        if (authenticationResult == null) {
                            LogUtil.logError(TAG, "authenticationResult is null");
                        } else {
                            isAuthSuccess.set(true);
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        LogUtil.logError(TAG, "MsalException exception=" + exception);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception exception) {
                        LogUtil.logError(TAG, "Exception exception=" + exception);
                        latch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        LogUtil.logError(TAG, "Cancel");
                        latch.countDown();
                    }
                });

                // lock the thread
                latch.await(TIMEOUT, TimeUnit.SECONDS);
            } else {
                LogUtil.logError(TAG, "no logging user");
            }

        } catch (MsalClientException e) {
            LogUtil.logError(TAG, "MSAL Exception Generated while getting users: " + e);
        } catch (IndexOutOfBoundsException e) {
            LogUtil.logError(TAG, "User at this position does not exist: " + e);
        } catch (IllegalStateException e) {
            LogUtil.logError(TAG, "MSAL Exception Generated: " + e);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException, e=" + e);
        } catch (Exception e) {
            LogUtil.logError(TAG, "Other Exception: " + e);
        }

        return isAuthSuccess.get();
    }

    @WorkerThread
    @UUIDStatusType
    public static int getUUIDStatus(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        final String address = PhoneUtil.getSKREmail(context);
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "address is null or empty");
            return UUIDStatusType.ERROR;
        }

        if (!NetworkUtil.isNetworkConnected(context)) {
            LogUtil.logInfo(TAG, "Network not available, skip");
            return UUIDStatusType.ERROR;
        }

        final AtomicInteger uuidStatus = new AtomicInteger(UUIDStatusType.ERROR);

        @DriveServiceType final int driveServiceType = DriveUtil.getServiceType(context);
        final DriveUtil util = DriveUtilFactory.getDriveUtil(context, address);
        switch (driveServiceType) {
            case DriveServiceType.googleDrive:
                // Google Drive, check UUID on Drive
                final CountDownLatch latch = new CountDownLatch(1);
                util.loadUUIDHash(new DriveCallback<String>() {
                    @Override
                    public void onComplete(String message) {
                        if (DriveUtil.isUUIDHashDifferentFromDrive(context, message)) {
                            LogUtil.logInfo(TAG, "UUID not match");
                            uuidStatus.set(UUIDStatusType.NOT_MATCH);
                        } else {
                            uuidStatus.set(UUIDStatusType.MATCH);
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LogUtil.logError(TAG, "loadUUIDHash(), error=" + e);
                        if (e instanceof DriveUtilException) {
                            LogUtil.logInfo(TAG, "It's instanceof DriveUtilException");
                            uuidStatus.set(UUIDStatusType.NOT_MATCH);
                        } else {
                            uuidStatus.set(UUIDStatusType.ERROR);
                        }
                        latch.countDown();
                    }
                });

                // lock the thread
                try {
                    latch.await(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LogUtil.logError(TAG, "InterruptedException e=" + e);
                }

                break;
            case DriveServiceType.oneDrive:
                // One Drive, check UUID on File
                if (DriveUtil.isUUIDHashDifferentFromLocalFile(context)) {
                    LogUtil.logInfo(TAG, "UUID not match");
                    uuidStatus.set(UUIDStatusType.NOT_MATCH);

                } else {
                    // Sync AlarmManager
                    DriveDataUpdateUtil driveDataUpdateUtil = new DriveDataUpdateUtil(context);
                    driveDataUpdateUtil.startSyncDriveData();

                    uuidStatus.set(UUIDStatusType.MATCH);
                }
                break;
            case DriveServiceType.undefined:
                LogUtil.logError(TAG, "undefined driveServiceType");
                break;
            default:
                LogUtil.logError(TAG, "unknown driveServiceType=" + driveServiceType);
        }

        return uuidStatus.get();
    }

    @WorkerThread
    public static boolean uploadTrustContacts(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        final String address = PhoneUtil.getSKREmail(context);
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "address is null or empty");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean isSuccess = new AtomicBoolean(false);

        final DriveUtil util = DriveUtilFactory.getDriveUtil(context, address);

        BackupTargetUtil.getAllOK(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {

                if (backupTargetEntityList == null) {
                    LogUtil.logError(TAG, "backupTargetEntityList is null",
                            new IllegalStateException("backupTargetEntityList is null"));
                    latch.countDown();
                    return;
                }

                List<BackupTargetForDrive> backupTargetForDriveList = new ArrayList<>();
                for (BackupTargetEntity backupTarget : backupTargetEntityList) {
                    BackupTargetForDrive backupTargetForDrive =
                            new BackupTargetForDrive(backupTarget.getName());
                    backupTargetForDriveList.add(backupTargetForDrive);
                }
                LogUtil.logDebug(TAG, "Trust contacts size=" + backupTargetForDriveList.size());

                final String message = DriveUtil.listToString(backupTargetForDriveList);
                // We use partial uuidHash as trust contacts file name prefix
                final String uuidHash = PhoneUtil.getSKRIDHash(context);
                final String filePrefix = DriveUtil.getFilePrefix(uuidHash);
                util.saveTrustContacts(filePrefix, message, new DriveCallback<Void>() {
                    @Override
                    public void onComplete(Void aVoid) {
                        LogUtil.logInfo(TAG, "Upload trust contacts success");
                        isSuccess.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LogUtil.logError(TAG, "Upload trust contacts failed, e=" + e);
                        latch.countDown();
                    }
                });
            }
        });

        // lock the thread
        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException e=" + e);
        }

        return isSuccess.get();
    }

    public static void checkDeprecatedTrustContactsFile(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        final String address = PhoneUtil.getSKREmail(context);
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "address is null or empty");
            return;
        }

        // GoogleDrive's trust contacts file name is different from OneDrive
        // Therefore, deprecated trust contacts file name is different either

        // Trust contacts file name
        // GoogleDrive : UUIDHash_trustContact
        // OneDrive    : UUIDHash_trustContact.txt
        final String deprecatedFileName;
        @DriveServiceType
        int serviceType = getServiceType(context);
        switch (serviceType) {
            case DriveServiceType.googleDrive:
                deprecatedFileName = TRUST_CONTACT_FILE_NAME;
                break;
            case DriveServiceType.oneDrive:
                deprecatedFileName = TRUST_CONTACT_FILE_NAME_TXT;
                break;
            case DriveServiceType.undefined:
                LogUtil.logWarning(TAG, "undefined drive service type");
                return;
            default:
                LogUtil.logWarning(TAG, "unknown drive service type=" + serviceType);
                return;
        }

        sCheckDeprecatedTrustContactsThread.execute(new Runnable() {
            @Override
            public void run() {
                // Lock the thread, prevent not necessary multiple thread check
                final CountDownLatch latch = new CountDownLatch(1);

                final DriveUtil driveUtil = DriveUtilFactory.getDriveUtil(context, address);
                // load deprecated trust contacts (without prefix)
                driveUtil.loadTrustContacts(null, new DriveCallback<String>() {
                    @Override
                    public void onComplete(String message) {

                        if (TextUtils.isEmpty(message)) {
                            LogUtil.logDebug(TAG, "Deprecated trust contacts file not found");
                            latch.countDown();
                        } else {
                            LogUtil.logDebug(TAG,
                                    "Deprecated trust contacts file found, delete it");
                            driveUtil.deleteFile(deprecatedFileName, new DriveCallback<Void>() {
                                @Override
                                public void onComplete(Void aVoid) {
                                    LogUtil.logInfo(TAG,
                                            "Deprecated trust contacts file deleted");
                                    latch.countDown();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    LogUtil.logDebug(TAG,
                                            "Deleting deprecated trust contacts file failed, e="
                                                    + e);
                                    latch.countDown();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LogUtil.logDebug(TAG, "load deprecated trust contacts failed, e=" + e);
                        latch.countDown();
                    }
                });

                try {
                    latch.await(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LogUtil.logError(TAG,
                            "checkDeprecatedTrustContactsFile InterruptedException e=" + e);
                }

            }
        });
    }

    public abstract void saveTrustContacts(
            @Nullable final String fileNamePrefix,
            final String trustContact,
            final DriveCallback<Void> callback);

    public abstract void saveUUIDHash(final String UUID, final DriveCallback<Void> callback);

    public abstract void loadTrustContacts(
            @Nullable final String fileNamePrefix, final DriveCallback<String> callback);

    public abstract void loadUUIDHash(final DriveCallback<String> callback);

    public abstract void deleteFile(String fileName, @NonNull DriveCallback<Void> callback);
}
