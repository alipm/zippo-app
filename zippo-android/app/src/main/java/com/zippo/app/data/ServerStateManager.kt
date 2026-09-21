import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServerStateManager {
    private val _isServerWakingUp = MutableStateFlow(false)

    // Observe this anywhere in your UI (Activities, Fragments, or Compose)
    val isServerWakingUp = _isServerWakingUp.asStateFlow()

    fun setWakingUp(waking: Boolean) {
        _isServerWakingUp.value = waking
    }
}
