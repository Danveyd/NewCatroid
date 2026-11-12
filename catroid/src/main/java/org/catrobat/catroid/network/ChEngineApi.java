package org.catrobat.catroid.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ChEngineApi {

    @POST("/api/authentication")
    Call<Object> login(@retrofit2.http.Body Object loginRequest);

    @Multipart
    @POST("/api/projects")
    Call<Void> uploadProject(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file,
            @Part("checksum") RequestBody checksum
    );
}