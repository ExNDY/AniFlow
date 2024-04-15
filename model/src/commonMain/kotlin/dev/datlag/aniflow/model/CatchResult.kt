package dev.datlag.aniflow.model

import dev.datlag.aniflow.model.CatchResult.Companion.result
import dev.datlag.aniflow.model.CatchResult.Success
import dev.datlag.tooling.async.suspendCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface CatchResult<T> {

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    fun onSuccess(callback: (T & Any) -> Unit) = apply {
        if (this is Success) {
            callback(this.data)
        }
    }

    fun asSuccess(onError: (Throwable?) -> T & Any): T & Any {
        return if (this is Success) {
            this.data
        } else {
            onError((this as? Error)?.throwable)
        }
    }

    fun asNullableSuccess(onError: (Throwable?) -> T? = { null }): T? {
        return if (this is Success) {
            this.data
        } else {
            onError((this as? Error)?.throwable)
        }
    }

    fun asSuccessOrThrow(): T & Any {
        return if (this is Success) {
            this.data
        } else {
            throw (this as Error).throwable ?: IllegalStateException()
        }
    }

    fun validate(predicate: (CatchResult<T>) -> Boolean): CatchResult<T> {
        return if (predicate(this)) {
            this
        } else {
            Error(null)
        }
    }

    fun validateSuccess(predicate: (T & Any) -> Boolean): CatchResult<T> {
        return when (this) {
            is Success -> {
                if (predicate(this.data)) {
                    this
                } else {
                    Error(null)
                }
            }
            else -> this
        }
    }

    suspend fun <M : Any> mapSuccess(block: suspend (T & Any) -> M?): CatchResult<M> {
        return when (this) {
            is Success -> {
                block(this.data)?.let(::Success) ?: Error(null)
            }
            is Error -> {
                Error(this.throwable)
            }
        }
    }

    data class Success<T>(
        val data: T & Any
    ) : CatchResult<T & Any>

    data class Error<T>(val throwable: Throwable?) : CatchResult<T>

    companion object {
        suspend fun <T> result(block: suspend CoroutineScope.() -> T): CatchResult<T & Any> = coroutineScope {
            val result = suspendCatching(block)
            return@coroutineScope if (result.isFailure) {
                Error(result.exceptionOrNull())
            } else {
                result.getOrNull()?.let {
                    Success(it)
                } ?: Error(result.exceptionOrNull())
            }
        }

        suspend fun <T> repeat(
            times: Int,
            delayDuration: Duration = 0.seconds,
            block: suspend CoroutineScope.() -> T
        ): CatchResult<T & Any> = coroutineScope {
            var result = suspendCatching(block)
            var request = 1

            while (result.isFailure && request < times) {
                delay(delayDuration)
                result = suspendCatching(block)
                request++
            }
            return@coroutineScope if (result.isFailure) {
                Error(result.exceptionOrNull())
            } else {
                result.getOrNull()?.let {
                    Success(it)
                } ?: Error(result.exceptionOrNull())
            }
        }
    }
}

suspend inline fun <T : Any> CatchResult<T>.resultOnError(noinline block: CoroutineScope.() -> T): CatchResult<out T> {
    return when (this) {
        is CatchResult.Error -> result(block)
        else -> this
    }
}

suspend inline fun <reified M : Any> CatchResult<*>.mapError(block: () -> M?): CatchResult<M> {
    return when (this) {
        is CatchResult.Error -> {
            block()?.let(::Success) ?: CatchResult.Error(null)
        }
        is Success -> {
            (this.data as? M)?.let(::Success) ?: block()?.let(::Success) ?: CatchResult.Error(null)
        }
    }
}

inline fun <T> CatchResult<T>.asError(onSuccess: () -> Throwable? = { null }): Throwable? {
    return if (this is CatchResult.Error) {
        this.throwable
    } else {
        onSuccess()
    }
}

inline fun <T> CatchResult<T>.onError(callback: (Throwable?) -> Unit) = apply {
    if (this is CatchResult.Error) {
        callback(this.throwable)
    }
}