package com.erdalgunes.fidan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.erdalgunes.fidan.data.ImpactData
import com.erdalgunes.fidan.data.ImpactRepository
import com.erdalgunes.fidan.data.Result

sealed class ImpactUiState {
    object Loading : ImpactUiState()
    data class Success(val data: ImpactData) : ImpactUiState()
    data class Error(val message: String, val errorType: ErrorType = ErrorType.GENERIC) : ImpactUiState()
}

enum class ErrorType {
    NETWORK,
    TIMEOUT,
    GENERIC
}

class ImpactViewModel(
    private val repository: ImpactRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ImpactUiState>(ImpactUiState.Loading)
    val uiState: StateFlow<ImpactUiState> = _uiState.asStateFlow()
    
    init {
        loadImpactData()
    }
    
    fun loadImpactData() {
        viewModelScope.launch {
            _uiState.value = ImpactUiState.Loading
            
            when (val result = repository.getImpactData()) {
                is Result.Success -> {
                    _uiState.value = ImpactUiState.Success(result.data)
                }
                is Result.Error -> {
                    val errorType = when {
                        result.message.contains("timeout", ignoreCase = true) -> ErrorType.TIMEOUT
                        result.message.contains("network", ignoreCase = true) -> ErrorType.NETWORK
                        else -> ErrorType.GENERIC
                    }
                    _uiState.value = ImpactUiState.Error(result.message, errorType)
                }
                is Result.Loading -> {
                    _uiState.value = ImpactUiState.Loading
                }
            }
        }
    }
    
    fun refresh() {
        loadImpactData()
    }
}

class ImpactViewModelFactory(
    private val repository: ImpactRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImpactViewModel::class.java)) {
            return ImpactViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}