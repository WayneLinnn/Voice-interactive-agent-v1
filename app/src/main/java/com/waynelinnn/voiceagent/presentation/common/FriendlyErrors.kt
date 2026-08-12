package com.waynelinnn.voiceagent.presentation.common

import androidx.annotation.StringRes
import com.waynelinnn.voiceagent.R

/** Maps raw engine/network errors into short user-facing copy. */
object FriendlyErrors {

    @StringRes
    fun assistantErrorRes(raw: String): Int? {
        val lower = raw.lowercase()
        return when {
            "api key" in lower || ("missing" in lower && "key" in lower) ->
                R.string.error_api_key
            "unable to resolve" in lower ||
                "unknownhost" in lower ||
                "failed to connect" in lower ||
                "network" in lower ||
                "unreachable" in lower ->
                R.string.error_network
            "timeout" in lower || "timed out" in lower ->
                R.string.error_timeout
            "401" in lower || "unauthorized" in lower || "invalid_api_key" in lower ->
                R.string.error_unauthorized
            "429" in lower || "rate limit" in lower ->
                R.string.error_rate_limit
            "503" in lower || "502" in lower || "overloaded" in lower ->
                R.string.error_server
            else -> null
        }
    }

    fun displayMessage(raw: String, fallback: String): String {
        // Prefer mapped string from caller; keep a short raw fallback here.
        return raw.trim().take(160).ifBlank { fallback }
    }
}
