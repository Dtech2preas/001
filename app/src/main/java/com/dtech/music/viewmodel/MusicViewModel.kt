package com.dtech.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dtech.music.network.SearchResult
import com.dtech.music.repository.MusicRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    private val repository = MusicRepository()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _streamUrl = MutableSharedFlow<String>()
    val streamUrl = _streamUrl.asSharedFlow()

    fun search(query: String) {
        viewModelScope.launch {
            val results = repository.search(query)
            _searchResults.value = results
        }
    }

    fun onPlayClicked(videoId: String) {
        viewModelScope.launch {
            val url = repository.getStreamUrl(videoId)
            if (url != null) {
                _streamUrl.emit(url)
            }
        }
    }
}
