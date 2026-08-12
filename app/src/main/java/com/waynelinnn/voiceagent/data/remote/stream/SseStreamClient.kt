package com.waynelinnn.voiceagent.data.remote.stream

import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/**
 * Shared SSE streaming helper for chat token events.
 */
class SseStreamClient(
    private val okHttpClient: OkHttpClient,
    private val eventSourceFactory: EventSource.Factory = EventSources.createFactory(okHttpClient),
) {
    fun stream(
        request: Request,
        parseEvent: (data: String) -> LlmStreamEvent?,
    ): Flow<LlmStreamEvent> = callbackFlow {
        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (data == "[DONE]") {
                    trySend(LlmStreamEvent.Completed)
                    close()
                    return
                }
                val parsed = runCatching { parseEvent(data) }.getOrElse { error ->
                    LlmStreamEvent.Error("Failed to parse SSE payload", error)
                }
                when (parsed) {
                    null -> Unit
                    is LlmStreamEvent.Completed -> {
                        trySend(parsed)
                        close()
                    }
                    is LlmStreamEvent.Error -> {
                        trySend(parsed)
                        close(parsed.cause)
                    }
                    is LlmStreamEvent.Token -> trySend(parsed)
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                val message = t?.message
                    ?: response?.message
                    ?: "SSE stream failed"
                trySend(LlmStreamEvent.Error(message, t))
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = eventSourceFactory.newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }
}
