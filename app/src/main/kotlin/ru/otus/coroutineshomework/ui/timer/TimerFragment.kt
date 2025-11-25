package ru.otus.coroutineshomework.ui.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.otus.coroutineshomework.databinding.FragmentTimerBinding
import java.util.Locale
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Фрагмент таймера. Для промыщленной реализации видимо стоит вынести
 * код таймера в отделный поток для предотвращения пауз при поворотах экрана
 * */
class TimerFragment : Fragment() {

    /** Приватная ссылка на привязку макета, используется для доступа к визуальным элементам интерфейса. Освобождается в onDestroyView. */
    private var _binding: FragmentTimerBinding? = null

    /** Безопасная ссылка на привязку макета. Доступна только после onCreateView и до onDestroyView. */
    private val binding get() = _binding!!

    /** Ссылка на запущенную корутину таймера, чтобы можно было отменить её при остановке. */
    private var timerCoroutineJob: Job? = null

    /** Приватный StateFlow для хранения текущего значения времени таймера. Обновляется напрямую через .value. */
    private val _timeFlow = MutableStateFlow(Duration.ZERO)

    /**
     * Публичный доступ к текущему значению времени таймера в виде [StateFlow].
     * Предоставляет только чтение, предотвращая внешнее изменение состояния.
     * Используется, например, для подписки извне на изменение времени таймера.
     * В данном приложении не требуетмя, оставил такой вариант для запоминания подхода
     */
    val timeFlow = _timeFlow.asStateFlow()

    /** Флаг состояния таймера (запущен/остановлен), с реактивным обновлением через observable-делегат. */
    private var started by Delegates.observable(false) { _, _, newValue ->
        setButtonsState(newValue)
        if (newValue) startTimer() else stopTimer()
    }

    /**
     * Настраивает состояние кнопок в зависимости от того, запущен ли таймер.
     *
     * @param started `true`, если таймер запущен; `false`, если остановлен.
     */
    private fun setButtonsState(started: Boolean) {
        with(binding) {
            btnStart.isEnabled = !started
            btnStop.isEnabled = started
        }
    }

    /**
     * Вызывается системой при создании View фрагмента.
     * Создаёт и возвращает корневой [View] на основе макета.
     * Переопределен для инициализации [_binding]
     *
     * @param inflater Инфлейтер для создания представления.
     * @param container Родительский контейнер, может быть null.
     * @param savedInstanceState Предыдущее состояние, если фрагмент пересоздаётся.
     * @return Корневая [View] фрагмента.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Вызывается после создания View. Здесь происходит инициализация:
     * - восстановление состояния таймера,
     * - подписка на обновления времени,
     * - настройка обработчиков кнопок.
     *
     * Использует [repeatOnLifecycle] для безопасной подписки на [timeFlow].
     *
     * @param view Корневой элемент интерфейса.
     * @param savedInstanceState Предыдущее состояние фрагмента.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация StateFlow с восстановлением состояния
        val initialTime = savedInstanceState?.getLong(TIME, 0) ?: 0
        _timeFlow.value = initialTime.milliseconds
        started = savedInstanceState?.getBoolean(STARTED) ?: false

        // Подписка на обновления времени через repeatOnLifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                timeFlow.collect {
                    binding.time.text = it.toDisplayString()
                }
            }
        }

        // Настройка кнопок
        binding.btnStart.setOnClickListener { started = true }
        binding.btnStop.setOnClickListener { started = false }
    }

    /**
     * Вызывается перед уничтожением фрагмента для сохранения текущего состояния.
     * Сохраняет прошедшее время и состояние таймера.
     *
     * @param outState Пакет для сохранения данных.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(TIME, _timeFlow.value.inWholeMilliseconds)
        outState.putBoolean(STARTED, started)
    }

    /**
     * Запускает корутину, которая эмулирует работу таймера:
     * каждые [TIMER_DELAY_MS] миллисекунд увеличивает значение [timeFlow].
     * Если корутина уже запущена — выходит без повторного старта.
     */
    private fun startTimer() {
        if (timerCoroutineJob?.isActive == true) return
        timerCoroutineJob = lifecycleScope.launch {
            while (started && isActive) {
                delay(TIMER_DELAY_MS)
                // По результатм дискуссий с ИИ убрал emit.
                // Мне данный способ эмпонирует тем, что код становится более читаем.
                // Аргументы от ИИ почему в данном случае можно заменить emit:
                // Прямое обновление значения через .value предпочтительнее, чем emit,
                // потому что:
                // - Это не suspend-выражение, что делает код проще и понятнее.
                // - Поведение идентично emit() для MutableStateFlow (внутри emit вызывает value = ...).
                // - Лучше соответствует семантике StateFlow как контейнера состояния.
                _timeFlow.value += timerDelayMs
            }
        }
    }

    /**
     * Останавливает таймер, отменяя запущенную корутину.
     * Это гарантирует, что цикл в [startTimer] немедленно прекратится.
     */
    private fun stopTimer() {
        timerCoroutineJob?.cancel()
        timerCoroutineJob = null
    }

    /**
     * Вызывается при уничтожении вида фрагмента.
     * Освобождает ссылку на [binding], предотвращая утечки памяти.
     */
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        /**
         * Ключ для сохранения текущего значения времени таймера (в миллисекундах) в [Bundle] при сохранении состояния фрагмента.
         */
        private const val TIME = "time"

        /**
         * Ключ для сохранения флага состояния таймера (запущен/остановлен) в [Bundle] при сохранении состояния фрагмента.
         */
        private const val STARTED = "started"

        /**
         * Задержка между обновлениями таймера в корутине, указана в миллисекундах.
         * Значение 16 мс соответствует примерно 60 обновлениям в секунду — достаточная частота для плавного отображения.
         */
        private const val TIMER_DELAY_MS = 16L

        /** Преобразованное значение [TIMER_DELAY_MS] в объект [Duration], используется для приращения времени таймера. */
        private val timerDelayMs = TIMER_DELAY_MS.milliseconds

        /** Количество полных секунд в одной минуте — используется для форматирования времени. */
        private val secondInMinute = 1.minutes.inWholeSeconds

        /** Количество полных миллисекунд в одной секунде — используется для форматирования времени. */
        private val millisecondInSecond = 1.seconds.inWholeMilliseconds

        /**
         * Форматирует [Duration] в строку формата "MM:SS.mmm" (минуты:секунды.миллисекунды).
         *
         * @receiver [Duration] — интервал времени.
         * @return Отформатированная строка для отображения в UI.
         */
        private fun Duration.toDisplayString(): String = String.format(
            Locale.getDefault(),
            "%02d:%02d.%03d",
            inWholeMinutes.toInt(),
            (inWholeSeconds % secondInMinute).toInt(),
            (inWholeMilliseconds % millisecondInSecond).toInt()
        )
    }
}