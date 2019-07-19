package com.htc.wallet.skrsdk.demoapp.whisper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.text.TextUtils;
import android.util.Log;

import com.htc.wallet.skrsdk.whisper.WhisperUtils;
import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhMessageBody;

import org.junit.Test;

import java.util.List;

// TODO Need to wait a valid signature to access ApiKeyAdapter.
public class WhisperUtilsTest {
    private static final String TAG = WhisperUtilsTest.class.getSimpleName();

    private static final String SHH_VER = "6.0";
    private static final String TEST_PASS = "test_pass";
    private static final String TEST_MSG = "test_msg";
    private static final String TEST_TARGET_PRV_KEY =
            "0x9ac90f0ae3ce818bd5e0a6ad34a6816e6f2ebd3a58f94589ccd77d433c5438aa";
    private static final String MAIL_SERVER_PEER =
            "enode://907b95c7a37e47ed828e71729be04b8c1664932543ae1e21bd3533ea247c5d74aebc58fd64ff40c85ae56e67f97a4b85fe40305c2b47ce75d8ee5df10e8c94e5@10.218.24.74:30303";

    @Test
    public void testShhMethod() {
        // To check if whisper server is alive.
        String whisperVer = WhisperUtils.shhVersion();
        Log.d(TAG, "whisperVer=" + whisperVer);
        assertEquals(SHH_VER, whisperVer);

        // To check if whisper server is alive.
        Object shhInfo = WhisperUtils.shhInfo();
        Log.d(TAG, "shhInfo=" + shhInfo);

        String keyPairId = WhisperUtils.shhNewKeyPair();
        if (TextUtils.isEmpty(keyPairId)) {
            fail("keyPairId is empty");
        }

        String pubKey = WhisperUtils.shhGetPublicKey(keyPairId);
        if (TextUtils.isEmpty(pubKey)) {
            fail("pubKey is empty");
        }

        String prvKey = WhisperUtils.shhGetPrivateKey(keyPairId);
        if (TextUtils.isEmpty(prvKey)) {
            fail("prvKey is empty");
        }

        String keyPairIdAfterAdded = WhisperUtils.shhAddPrivateKey(prvKey);
        assertEquals(keyPairId, keyPairIdAfterAdded);

        String senderKeyPairId = WhisperUtils.shhAddPrivateKey(TEST_TARGET_PRV_KEY);
        if (TextUtils.isEmpty(senderKeyPairId)) {
            fail("senderKeyPairId is empty");
        }

        boolean hasKeyPair = WhisperUtils.shhHasKeyPair(keyPairId);
        assertTrue(hasKeyPair);

        // TODO Should we use the real password?
        String symKeyIdFromPass = WhisperUtils.shhGenerateSymKeyFromPassword(TEST_PASS);
        if (TextUtils.isEmpty(symKeyIdFromPass)) {
            fail("symKeyIdFromPass is empty");
        }

        String msgFilterId = WhisperUtils.shhNewMessageFilter(keyPairId, null);
        if (TextUtils.isEmpty(msgFilterId)) {
            fail("shhNewMessageFilter failed");
        }

        List<ShhMessageBody.ResultObj> messages = WhisperUtils.shhGetFilterMessages(msgFilterId);
        assertNotNull(messages);

        String requestMsg = WhisperUtils.shhRequestMessage(MAIL_SERVER_PEER, symKeyIdFromPass);
        if (TextUtils.isEmpty(requestMsg)) {
            fail("shhRequestMessage failed");
        }

        String postResult = WhisperUtils.shhPost(pubKey, senderKeyPairId, TEST_MSG);
        if (TextUtils.isEmpty(postResult)) {
            fail("postResult failed");
        }

        for (int i = 0; i < 100; i++) {
            WhisperUtils.registerFilterMessages(msgFilterId, new WhisperUtils.WhisperListener() {
                @Override
                public void onMessageReceived(String message, long timeStamp) {

                }
            });
            WhisperUtils.unRegisterFilterMessages(msgFilterId);
        }

        boolean delKeyPair = WhisperUtils.shhDeleteKeyPair(keyPairId);
        assertTrue(delKeyPair);

        boolean delSymKey = WhisperUtils.shhDeleteSymKey(symKeyIdFromPass);
        assertTrue(delSymKey);

        boolean delFilter = WhisperUtils.shhDeleteMessageFilter(msgFilterId);
        assertTrue(delFilter);
    }
}
