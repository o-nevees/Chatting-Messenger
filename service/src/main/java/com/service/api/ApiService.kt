package com.service.api

import okhttp3.RequestBody
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun loginWithToken(
        @Field("token") firebaseToken: String,
        @Field("id_device") deviceId: String,
        @Field("device_name") deviceName: String?,
        @Field("fmc_token") fmcToken: String?
    ): Response<ApiResponse<AuthResponseData>>

    @FormUrlEncoded
    @POST("profile/create")
    suspend fun createProfile(
        @Field("firebase_token") firebaseToken: String,
        @Field("user_name") name: String,
        @Field("user_username") username: String,
        @Field("birthdate") birthdate: String,
        @Field("device_name") deviceName: String,
        @Field("id_device") deviceId: String,
        @Field("fmc_token") fmcToken: String?
    ): Response<ApiResponse<CreateProfileResponseData>>

    @FormUrlEncoded
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String
    ): Response<ApiResponse<RefreshTokenData>>

    @GET("auth/validate")
    suspend fun validateToken(
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<ValidateTokenData>>

    @GET("profile/checkUsername")
    suspend fun checkUsername(
        @Query("username") username: String
    ): Response<ApiResponse<UsernameAvailabilityData>>

    @Multipart
    @POST("uploads/profilePhoto")
    suspend fun uploadProfilePhoto(
        @Header("Authorization") authToken: String,
        @Part photo: MultipartBody.Part
    ): Response<ApiResponse<UploadPhotoData>>

    @FormUrlEncoded
    @PUT("profile")
    suspend fun updateProfile(
        @Header("Authorization") authToken: String,
        @Field("user_name") name: String,
        @Field("user_username") username: String,
        @Field("bio") bio: String
    ): Response<SimpleApiResponse>

    @GET("search")
    suspend fun searchEntities(
        @Header("Authorization") authToken: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<SearchResult>>>
}