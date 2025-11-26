package ru.otus.coroutineshomework.ui.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.otus.coroutineshomework.databinding.FragmentTimerBinding
import java.util.Locale
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private var timerJob: Job? = null

//    private var time: Duration by Delegates.observable(Duration.ZERO) { _, _, newValue ->
//        binding.time.text = newValue.toDisplayString()
//    }

    private var timeFlow: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)

    private var started by Delegates.observable(false) { _, _, newValue ->
        setButtonsState(newValue)
        if (newValue) {
            startTimer()
        } else {
            stopTimer()
        }
    }

    private fun setButtonsState(started: Boolean) {
        with(binding) {
            btnStart.isEnabled = !started
            btnStop.isEnabled = started
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            timeFlow.value = it.getLong(TIME).milliseconds
            started = it.getBoolean(STARTED)
        }
        setButtonsState(started)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                timeFlow.collect { time ->
                    binding.time.text = time.toDisplayString()
                }
            }
        }

        with(binding) {
            btnStart.setOnClickListener {
                started = true
            }
            btnStop.setOnClickListener {
                started = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(TIME, timeFlow.value.inWholeMilliseconds)
        outState.putBoolean(STARTED, started)
    }

    private fun startTimer() {
        timerJob = lifecycleScope.launch {
            while (isActive) {
                delay(TIMER_DELAY_MS) // Обновление примерно 60 раз в секунду (~16.67 мс)
                timeFlow.emit(timeFlow.value + timerDelayMs)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TIME = "time"
        private const val STARTED = "started"

        private const val TIMER_DELAY_MS = 16L

        private val timerDelayMs = TIMER_DELAY_MS.milliseconds

        private val secondInMinute = 1.minutes.inWholeSeconds
        private val millisecondInSecond = 1.seconds.inWholeMilliseconds

        private fun Duration.toDisplayString(): String = String.format(
            Locale.getDefault(),
            "%02d:%02d.%03d",
            inWholeMinutes.toInt(),
            (inWholeSeconds % secondInMinute).toInt(),
            (inWholeMilliseconds % millisecondInSecond).toInt()
        )
    }
}