package org.example.moxia.api

import org.example.moxia.model.CurrentSessionResponse
import org.example.moxia.model.ListUsersResponse
import org.example.moxia.model.SignInRequestV0260
import org.example.moxia.model.SignInResponse
import org.example.moxia.model.UseRole
import org.example.moxia.model.UseState
import org.example.moxia.model.User
import retrofit2.http.GET

interface MemosApiV0260 : MemosApiV0353 {

    // Replaces getCurrentSession
    // https://demo.usememos.com/api/v1/auth/me
    @GET("auth/me")
    suspend fun getCurrentUser(): GetCurrentUserResponseDtoV0260

    @retrofit2.http.POST("auth/signin")
    suspend fun signIn(@retrofit2.http.Body request: SignInRequestV0260): SignInResponseDtoV0260
}

data class UserV0260(
    override val name: String? = null,
    override val role: UseRole? = null,
    override val username: String? = null,
    override val email: String? = null,
    override val displayName: String? = null,
    override val avatarUrl: String? = null,
    override val description: String? = null,
    override val password: String? = null,
    override val state: UseState? = null,
    override val createTime: String? = null,
    override val updateTime: String? = null,
    override val token: String? = null
) : User

data class GetCurrentUserResponseDtoV0260(val user: UserV0260) {
    fun toModel(): CurrentSessionResponse = CurrentSessionResponse(user = user)
}

data class SignInResponseDtoV0260(
    val user: UserV0260,
    val accessToken: String,
    val accessTokenExpiresAt: String
) {
    fun toModel(): SignInResponse = SignInResponse(
        user = user,
        accessToken = accessToken,
        accessTokenExpiresAt = accessTokenExpiresAt
    )
}

data class ListUsersResponseV0260(
    val users: List<UserV0260>?,
    val nextPageToken: String? = null,
    val totalSize: Int? = null
) {
    fun toModel(): ListUsersResponse = ListUsersResponse(
        users = users,
        nextPageToken = nextPageToken,
        totalSize = totalSize
    )
}
