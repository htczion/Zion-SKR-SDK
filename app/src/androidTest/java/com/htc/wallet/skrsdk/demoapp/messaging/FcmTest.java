package com.htc.wallet.skrsdk.demoapp.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.messaging.MessagingResult;
import com.htc.wallet.skrsdk.messaging.UserTokenListener;
import com.htc.wallet.skrsdk.messaging.message.Message;
import com.htc.wallet.skrsdk.messaging.processor.FirebaseMessageProcessor;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FcmTest {
    private static final String SENDER_TOKEN =
            "e13gSWTA07U:APA91bETV8E4GvCU2qzuSmLwrJCRf7UNXjWtFqogtFii1GJl77H37EdhuIoN"
                    + "-0UTGxvZRXsljqyuBv8hft2E8WHBA8OB1sjt2FLqpl2EIGP0sETFslIIk3QoRhZUYvA-wkm-25IXX2Hh";
    private static final String RECEIVER_TOKEN =
            "cgOIwGpnq0c:APA91bG7KTiBvzUSrlAmydpQJXQKCE7soHFy8pxTJ4EL1ZQj0VZ5jMLon7-X"
                    + "-mBFbPG01xqPT50dTRZJiYhLusg5FRz7R7Fi9QdfhvI4rhWFKEx00q2BrExxHHJ_K6lLh1PApE9d3Zg8";
    private static final String MESSAGE = "This is for test.";
    private static final String KEY = "Key1234";
    private static final String NOTIFICATION_TITLE = "New request from Amy.";
    private final FirebaseMessageProcessor mFirebaseMessageProcessor =
            (FirebaseMessageProcessor) MessageProcessorFactory.getInstance().getMessageProcessor(
                    MessageProcessorFactory.FIREBASE_MESSAGE_PROCESSOR);
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    @Test
    public void getFCMTokenTest() {
        mFirebaseMessageProcessor.getUserToken(mContext, new UserTokenListener() {
            @Override
            public void onUserTokenReceived(@NonNull String token) {
                assertNotNull(token);
                assertEquals(token, mFirebaseMessageProcessor.getFcmTokenFromSp(mContext));
            }

            @Override
            public void onUserTokenError(Exception exception) {

            }
        });
    }

    @Test
    public void putMessageTest() {
        Message message = new Message(SENDER_TOKEN, RECEIVER_TOKEN,
                MessageConstants.TYPE_BACKUP_VERIFY, MESSAGE);
        message.setKey(KEY);
        message.setNotificationTitle(NOTIFICATION_TITLE);
        assertEquals(message.getSender(), SENDER_TOKEN);
        assertEquals(message.getReceiver(), RECEIVER_TOKEN);
        assertEquals(message.getMessage(), MESSAGE);
        assertEquals(message.getMessageType(), MessageConstants.TYPE_BACKUP_VERIFY);
        assertEquals(message.getKey(), KEY);
        assertEquals(message.getNotificationTitle(), NOTIFICATION_TITLE);
    }

    @Test
    public void sendMessageTest() {
        Message message = new Message(SENDER_TOKEN, RECEIVER_TOKEN,
                MessageConstants.TYPE_BACKUP_VERIFY, MESSAGE);
        int result = mFirebaseMessageProcessor.sendMessage(mContext, message);
        assertEquals(result, MessagingResult.SUCCESS);
    }
}
