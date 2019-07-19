package com.htc.wallet.skrsdk.keyserver;

import com.htc.wallet.skrsdk.keyserver.requestbody.AttestationsRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.BackupCodePkRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.GetNonceRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.RestoreCodeRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.RestoreSeedRequestBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.AttestationsResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.BackupCodePkResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.GetNonceResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.RestoreCodeResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.RestoreSeedResponseBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface KeyServerApiService {

    @Deprecated
    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v1/encryptCodePK")
    Call<BackupCodePkResponseBody> postEncryptCodePK(@Body BackupCodePkRequestBody body);

    @Deprecated
    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v1/verifyCode")
    Call<RestoreCodeResponseBody> postVerifyCode(@Body RestoreCodeRequestBody body);

    @Deprecated
    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v1/encryptSeed")
    Call<RestoreSeedResponseBody> postEncryptSeed(@Body RestoreSeedRequestBody body);

    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v2/deviceVerify/android/getNonce")
    Call<GetNonceResponseBody> getNonceV2(@Body GetNonceRequestBody body);

    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v2/attestationsResult/android")
    Call<AttestationsResponseBody> postAttestationsResultV2(@Body AttestationsRequestBody body);

    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v2/encryptCodePK")
    Call<BackupCodePkResponseBody> postEncryptCodePKV2(
            @Header("Authorization") String attestToken, @Body BackupCodePkRequestBody body);

    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v2/verifyCode")
    Call<RestoreCodeResponseBody> postVerifyCodeV2(
            @Header("Authorization") String attestToken, @Body RestoreCodeRequestBody body);

    @Headers("Content-Type: application/json")
    @POST("exodus/kws/v2/encryptSeed")
    Call<RestoreSeedResponseBody> postEncryptSeedV2(
            @Header("Authorization") String attestToken, @Body RestoreSeedRequestBody body);
}
