package org.example.moxia.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.example.moxia.api.MemosApiFactory
import org.example.moxia.api.SsoException
import org.example.moxia.api.SsoFailure
import org.example.moxia.api.SsoTransaction
import org.example.moxia.api.createTokenFromSession
import org.example.moxia.model.SSOCredentials
import org.example.moxia.model.SignInRequest

sealed interface SsoOutcome {
    data class Success(val baseUrl: String, val token: String) : SsoOutcome
    data class Failure(val reason: SsoFailure) : SsoOutcome
    data object Cancelled : SsoOutcome
}

/** Retains the transaction across activity recreation, but never across process death. */
class SsoLoginViewModel(
    private val authenticate: suspend (SsoTransaction, SSOCredentials) -> String = { transaction, credentials ->
        val api = MemosApiFactory.create(transaction.baseUrl, OkHttpClient())
        val session = api.signIn(SignInRequest(ssoCredentials = credentials))
        if (session.accessToken.isBlank()) throw SsoException(SsoFailure.SIGN_IN)
        try {
            createTokenFromSession(transaction.baseUrl, session.accessToken)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            throw SsoException(SsoFailure.TOKEN)
        }
    }
) : ViewModel() {
    private val mutableOutcome = MutableStateFlow<SsoOutcome?>(null)
    val outcome = mutableOutcome.asStateFlow()
    var transaction: SsoTransaction? = null
        private set
    var browserLaunched = false
    var browserWasPaused = false
    var exchanging = false
        private set
    private var exchangeJob: Job? = null

    fun begin(transaction: SsoTransaction) {
        check(this.transaction == null)
        this.transaction = transaction
    }

    fun callback(uri: String) {
        if (exchanging || mutableOutcome.value != null) return
        val pending = transaction
        if (pending == null) {
            fail(SsoFailure.EXPIRED)
            return
        }
        val credentials = try {
            pending.consume(uri)
        } catch (error: SsoException) {
            fail(error.failure)
            return
        }
        exchanging = true
        exchangeJob = viewModelScope.launch {
            try {
                val token = authenticate(pending, credentials)
                check(token.isNotBlank())
                mutableOutcome.value = SsoOutcome.Success(pending.baseUrl, token)
            } catch (error: CancellationException) {
                throw error
            } catch (error: SsoException) {
                fail(error.failure)
            } catch (_: Exception) {
                fail(SsoFailure.SIGN_IN)
            } finally {
                transaction = null
                exchanging = false
            }
        }
    }

    fun fail(reason: SsoFailure) {
        transaction = null
        mutableOutcome.value = SsoOutcome.Failure(reason)
    }

    fun cancel() {
        exchangeJob?.cancel()
        transaction = null
        mutableOutcome.value = SsoOutcome.Cancelled
    }
}
