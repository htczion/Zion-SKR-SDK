package com.htc.wallet.skrsdk.whisper.retrofit.jsonbody;

import com.google.gson.annotations.SerializedName;

public class ShhResponseBody {
    @SerializedName("result")
    private Object mResult;

    @SerializedName("error")
    private Object mError;

    public Object getResult() {
        return mResult;
    }

    public Object getError() {
        return mError;
    }
}
