package com.service.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: T?,
    @SerializedName("message") val message: String?,
    @SerializedName("code") val code: String?
)

data class AuthResponseData(
    @SerializedName("is_new_user") val isNewUser: Boolean?,
    @SerializedName("auth_token") val authToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("user_data") val userData: UserLoginData?
)

data class UserLoginData(
    @SerializedName("number") val number: String?,
    @SerializedName("username1") val username1: String?,
    @SerializedName("username2") val username2: String?,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String?,
    @SerializedName("profile_photo_filename") val profilePhotoFilename: String?
)

data class RefreshTokenData(
    @SerializedName("auth_token") val authToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?
)

data class ValidateTokenData(
    @SerializedName("user_number") val userNumber: String?
)

typealias CreateProfileResponseData = RefreshTokenData

data class UsernameAvailabilityData(
    @SerializedName("available") val available: Boolean,
    @SerializedName("message") val message: String?
)

data class UploadPhotoData(
    @SerializedName("url") val url: String?,
    @SerializedName("filename") val filename: String?
)

data class SearchResponseData(
     val results: List<SearchResult>?
)

data class SearchResult(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("subtitle") val subtitle: String?
)

data class SimpleApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?
)