package com.example.service

import com.example.model.IncomingCall
import com.example.model.IslandConfig
import com.example.model.IslandNotification
import com.example.model.MediaTrack
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object AuraEventBus {
    private val _config = MutableStateFlow(IslandConfig())
    val config = _config.asStateFlow()

    fun updateConfig(c: IslandConfig) {
        _config.value = c
    }

    private val _notifications = MutableSharedFlow<IslandNotification>(extraBufferCapacity = 8)
    val notifications = _notifications.asSharedFlow()

    private val _calls = MutableSharedFlow<IncomingCall>(extraBufferCapacity = 4)
    val calls = _calls.asSharedFlow()

    private val _media = MutableSharedFlow<MediaTrack>(extraBufferCapacity = 8)
    val media = _media.asSharedFlow()

    suspend fun postNotification(n: IslandNotification) = _notifications.emit(n)
    suspend fun postCall(c: IncomingCall) = _calls.emit(c)
    suspend fun postMedia(m: MediaTrack) = _media.emit(m)

    fun tryPostNotification(n: IslandNotification) = _notifications.tryEmit(n)
    fun tryPostCall(c: IncomingCall) = _calls.tryEmit(c)
    fun tryPostMedia(m: MediaTrack) = _media.tryEmit(m)
}
