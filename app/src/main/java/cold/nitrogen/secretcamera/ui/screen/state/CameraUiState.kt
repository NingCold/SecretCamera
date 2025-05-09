package cold.nitrogen.secretcamera.ui.screen.state

enum class CameraMode{
    PHOTO, VIDEO
}

data class CameraUiState(
    val isRecording: Boolean = false,
    val mode: CameraMode = CameraMode.PHOTO,
)
