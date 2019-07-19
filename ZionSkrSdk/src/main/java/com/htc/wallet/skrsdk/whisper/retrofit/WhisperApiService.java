package com.htc.wallet.skrsdk.whisper.retrofit;

import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhMessageBody;
import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhRequestBody;
import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhResponseBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface WhisperApiService {

    @POST("/status-go")
    Call<ShhResponseBody> shhMethod(@Body ShhRequestBody shhRequestBody);

    @POST("/status-go")
    Call<ShhMessageBody> shhGetFilterMessages(@Body ShhRequestBody shhRequestBody);
}
