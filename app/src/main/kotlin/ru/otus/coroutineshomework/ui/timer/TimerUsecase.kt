package ru.otus.coroutineshomework.ui.timer

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


class TimerUseCase : CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob()

    private val _timeFlow: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    val timeFlow = _timeFlow.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        if (timerJob != null) return
        timerJob = launch {
            while (isActive) {
                delay(TIMER_DELAY_MS) // Обновление примерно 60 раз в секунду (~16.67 мс)
                Log.i("TimerUseCase", "Timer updated ${timeFlow.value + timerDelayMs}")
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

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun timerUseCase() = TimerUseCase()

}