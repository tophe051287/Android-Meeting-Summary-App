package com.ramonapps.meetingscribe.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramonapps.meetingscribe.MeetingScribeApp
import com.ramonapps.meetingscribe.data.Meeting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MeetingDetailViewModel(application: Application, meetingId: Long) : AndroidViewModel(application) {

    private val repository = (application as MeetingScribeApp).repository

    val meeting: StateFlow<Meeting?> = repository.observeById(meetingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    class Factory(private val application: Application, private val meetingId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MeetingDetailViewModel(application, meetingId) as T
        }
    }
}
