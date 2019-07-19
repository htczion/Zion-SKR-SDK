package com.htc.wallet.skrsdk.demoapp;

import static android.content.pm.PackageManager.GET_SIGNATURES;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignTest {
    private static final String TAG = "SignTest";

    private static final String SIGN_SHA1 = "7AF4E62666822575432041EFEBDFEC3D871ACD99";
    private static final String SIGN_SHA256 =
            "C69821055CDB316A28B0350E265D7FB6B1D75E54810629546A70528B055EFEB9";

    @Test
    public void signatureTest() {
        Context context = InstrumentationRegistry.getTargetContext();

        assertEquals(SIGN_SHA1, getSignatureSha1(context));
        assertEquals(SIGN_SHA256, getSignatureSha256(context));
    }

    private static String getSignatureSha1(Context context) {
        Signature signature = getSignature(context);
        if (signature == null) {
            return "";
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            return Hex.encodeToUppercase(messageDigest.digest(signature.toByteArray()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String getSignatureSha256(Context context) {
        Signature signature = getSignature(context);
        if (signature == null) {
            return "";
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return Hex.encodeToUppercase(messageDigest.digest(signature.toByteArray()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Signature getSignature(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            String packageName = context.getPackageName();
            PackageInfo pInfo = packageManager.getPackageInfo(packageName, GET_SIGNATURES);
            return pInfo.signatures[0];
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
