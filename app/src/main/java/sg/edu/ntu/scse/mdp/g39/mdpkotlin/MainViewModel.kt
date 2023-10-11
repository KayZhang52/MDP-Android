package sg.edu.ntu.scse.mdp.g39.mdpkotlin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val isConnected:Boolean = false,
    val connectionStatus:String = "Not Connected",
    val connectionStatusTextColor: Int = R.color.colorLabelNotConnected,
    val robotXCoordinate: Int = 0,
    val robotYCoordinate: Int = 0,
    val robotDirection: String = "North"
)



class MainViewModel: ViewModel() {
    // Expose screen UI state
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Handle business logic
    fun updateConnectionStatus(deviceName:String?, isConnected:Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                connectionStatus = if (deviceName != null) "Connected to $deviceName" else "Connected to Unknown" ,
                connectionStatusTextColor = if (isConnected) R.color.colorLabelNotConnected else R.color.colorLabelConnected,
                isConnected = isConnected
            )
        }
    }
}