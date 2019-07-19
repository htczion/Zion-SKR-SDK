package com.htc.wallet.skrsdk.whisper.retrofit.jsonbody;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;

import java.util.List;

/** For the return message from the method {@link WhisperUtils#shhGetFilterMessages(String)} */
public class ShhMessageBody {
    @SerializedName("result")
    private List<ResultObj> mResults;

    @SerializedName("error")
    private Object mError;

    public List<ResultObj> getResults() {
        return mResults;
    }

    public Object getError() {
        return mError;
    }

    public class ResultObj {
        @SerializedName("sig")
        private String mSig;

        @SerializedName("timestamp")
        private long mTimeStamp;

        @SerializedName("payload")
        private String mPayload;

        @SerializedName("recipientPublicKey")
        private String mRecipientPubKey;

        public String getSig() {
            return mSig;
        }

        public long getTimeStamp() {
            return mTimeStamp;
        }

        public String getPayload() {
            return mPayload;
        }

        public String getRecipientPubKey() {
            return mRecipientPubKey;
        }
    }
}
