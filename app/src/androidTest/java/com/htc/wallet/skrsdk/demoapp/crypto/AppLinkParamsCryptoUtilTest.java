package com.htc.wallet.skrsdk.demoapp.crypto;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.htc.wallet.skrsdk.applink.crypto.AppLinkParamsCryptoUtil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppLinkParamsCryptoUtilTest {

    @Test
    public void testEncryptDecrypt() {
        Context context = InstrumentationRegistry.getTargetContext();
        String plainText = "HTCWallet";
        AppLinkParamsCryptoUtil util = new AppLinkParamsCryptoUtil(context);
        String cipherText = util.encrypt(plainText);
        Assert.assertEquals(util.decrypt(cipherText), plainText);
    }
}
