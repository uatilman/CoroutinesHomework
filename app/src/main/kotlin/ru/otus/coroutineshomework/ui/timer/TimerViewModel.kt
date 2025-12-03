package ru.otus.coroutineshomework.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@ViewModelScoped
class TimerViewModel @Inject constructor() : ViewModel() {

    private val _timeFlow: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    val timeFlow = _timeFlow.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(TIMER_DELAY_MS) // Обновление примерно 60 раз в секунду (~16.67 мс)
                _timeFlow.emit(timeFlow.value + timerDelayMs)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    companion object {

        private const val TIMER_DELAY_MS = 16L

        private val timerDelayMs = TIMER_DELAY_MS.milliseconds


    }

}