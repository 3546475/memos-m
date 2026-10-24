package org.example.moxia.api

import org.example.moxia.model.ListAllUserStatsResponse
import org.example.moxia.model.PasswordCredentials
import org.example.moxia.model.SignInRequest
import org.example.moxia.model.SignInResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** REST schema introduced when Memos removed the /api/v1 version marker. */
interface MemosApiUnversioned : MemosApiV0300 {
    @GET("users/-/stats")
    override suspend fun listAllUserStats(): ListAllUserStatsResponse

    @POST("auth/signin")
    suspend fun signInUnversioned(@Body request: SignInRequestUnversioned): SignInResponseDtoV0280
}

data class SignInRequestUnversioned(
    val passwordCredentials: PasswordCredentials?,
    val ssoCredentials: SsoCredentialsUnversioned?
)

data class SsoCredentialsUnversioned(
    val identityProvider: String,
    val code: String,
    val redirectUri: String,
    val codeVerifier: String
)

class MemosApiUnversionedImpl(
    private val api: MemosApiUnversioned,
    private val delegate: MemosApi = MemosApiV0300Impl(api)
) : MemosApi by delegate {
    override val constants = delegate.constants.copy(apiPath = "api")

    override suspend fun signIn(request: SignInRequest): SignInResponse =
        api.signInUnversioned(SignInRequestUnversioned(
            request.passwordCredentials,
            request.ssoCredentials?.let {
                SsoCredentialsUnversioned(it.idpName, it.code, it.redirectUri, it.codeVerifier)
            }
        )).toModel()
}
