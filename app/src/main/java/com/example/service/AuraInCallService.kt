package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import com.example.model.IncomingCall
import com.example.model.IslandMode

/**
 * AURA In-Call Service — replaces the system phone call screen.
 *
 * When registered as the default dialer/phone app, Android routes all incoming
 * and outgoing call UI through this service instead of the system Phone app.
 * AURA suppresses the default full-screen call UI and instead triggers
 * the Dynamic Island CALL expansion via AuraEventBus.
 */
@RequiresApi(Build.VERSION_CODES.M)
class AuraInCallService : InCallService() {

    companion object {
        @Volatile
        var activeCall: Call? = null
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            when (state) {
                Call.STATE_DISCONNECTED,
                Call.STATE_DISCONNECTING -> {
                    activeCall = null
                    // Signal overlay to collapse
                    val vm = com.example.viewmodel.AuraViewModel.activeInstance
                    vm?.collapseToCompact()
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCall = call
        call.registerCallback(callCallback)

        // Extract caller info
        val details = call.details
        val handle = details?.handle
        val number = handle?.schemeSpecificPart ?: "Unknown"
        val callerName = details?.callerDisplayName?.toString()?.ifBlank { number } ?: number

        // Route to AURA Dynamic Island instead of system call screen
        AuraEventBus.tryPostCall(IncomingCall(callerName = callerName, callerNumber = number))
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (activeCall === call) {
            activeCall = null
        }
    }

    /**
     * Accept the current ringing call via AURA island action button.
     */
    fun acceptCurrentCall() {
        activeCall?.let { call ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            }
        }
    }

    /**
     * Reject/decline the current ringing call via AURA island action button.
     */
    fun declineCurrentCall() {
        activeCall?.let { call ->
            call.reject(false, null)
        }
    }
}
