package com.nehal.app.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nehal.app.data.FrameItem
import com.nehal.app.data.MediaStoreSaver
import com.nehal.app.data.VideoFrameExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Idle : UiState
    data class Loading(val message: String) : UiState
    data class Success(val message: String) : UiState
    data class Error(val error: String) : UiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val extractor = VideoFrameExtractor(application)
    private val saver = MediaStoreSaver(application)

    private val _frames = MutableStateFlow<List<FrameItem>>(emptyList())
    val frames: StateFlow<List<FrameItem>> = _frames.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun extractFrames(videoUri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading("Extracting frames via FFmpeg...")
            try {
                val result = extractor.extractAllFrames(videoUri)
                _frames.value = result
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to extract frames")
            }
        }
    }

    fun toggleFrameSelection(id: Int) {
        _frames.value = _frames.value.map { item ->
            if (item.id == id) item.copy(isSelected = !item.isSelected) else item
        }
    }

    fun selectAll(select: Boolean) {
        _frames.value = _frames.value.map { it.copy(isSelected = select) }
    }

    fun saveSelectedFrames() {
        val selectedFiles = _frames.value.filter { it.isSelected }.map { it.file }
        if (selectedFiles.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading("Saving ${selectedFiles.size} frames to Gallery...")
            try {
                val savedCount = saver.saveFramesToGallery(selectedFiles)
                _uiState.value = UiState.Success("Saved $savedCount frames to Pictures/NehalApp")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to save frames")
            }
        }
    }

    fun resetStateMessage() {
        _uiState.value = UiState.Idle
    }
}
