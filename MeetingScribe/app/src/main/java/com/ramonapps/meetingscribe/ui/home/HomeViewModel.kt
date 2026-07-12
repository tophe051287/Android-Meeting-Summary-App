package com.ramonapps.meetingscribe.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ramonapps.meetingscribe.MeetingScribeApp
import com.ramonapps.meetingscribe.data.Meeting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MeetingScribeApp).repository

    val meetings: StateFlow<List<Meeting>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMeeting(meeting: Meeting) {
        viewModelScope.launch { repository.deleteMeeting(meeting) }
    }
}
