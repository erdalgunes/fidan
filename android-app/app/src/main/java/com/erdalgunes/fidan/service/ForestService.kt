package com.erdalgunes.fidan.service

import com.erdalgunes.fidan.forest.ForestState
import com.erdalgunes.fidan.forest.Tree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForestService @Inject constructor() {
    
    private val _forestState = MutableStateFlow(
        ForestState(
            trees = emptyList(),
            isDayTime = true
        )
    )
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()
    
    fun updateDayNightCycle() {
        _forestState.value = _forestState.value.copy(
            isDayTime = !_forestState.value.isDayTime
        )
    }
}