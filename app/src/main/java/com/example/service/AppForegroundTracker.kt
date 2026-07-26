package com.example.service

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppForegroundTracker {
    private val _isAppForeground = MutableStateFlow(false)
    val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()

    fun attach() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { _isAppForeground.value = true }
            override fun onStop(owner: LifecycleOwner) { _isAppForeground.value = false }
        })
    }
}
