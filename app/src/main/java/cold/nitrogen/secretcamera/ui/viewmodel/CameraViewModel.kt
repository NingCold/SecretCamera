package cold.nitrogen.secretcamera.ui.viewmodel

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cold.nitrogen.secretcamera.data.respository.CameraRepository
import cold.nitrogen.secretcamera.ui.screen.state.CameraUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: CameraRepository
) : ViewModel() {

    var uiState by mutableStateOf(CameraUiState())
        private set

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(context)
        imageCapture = ImageCapture.Builder().build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(context: Context) {
        val imageCapture = imageCapture?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SecretCamera")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        // 使用协程异步保存图片
        viewModelScope.launch {
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Log.e("SecretCamera", "Photo capture failed: ${exception.message}", exception)
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.d("SecretCamera", "Photo capture saved succeeded!")
                    }
                }
            )
        }
    }

    fun captureVideo(context: Context) {
        val videoCapture = videoCapture?: return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "请授予相机权限", Toast.LENGTH_SHORT).show()
            return
        }

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            uiState = uiState.copy(isRecording = false)
            Toast.makeText(context, "Recording?", Toast.LENGTH_SHORT).show()
            return
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SecretCamera")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        uiState = uiState.copy(isRecording = true)

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.d("SecretCamera", "开始录像")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            Log.e("SecretCamera", "录像失败: ${event.error}")
                        } else {
                            Log.d("SecretCamera", "录像完成: ${event.outputResults.outputUri}")
                            Toast.makeText(context, "录像已保存", Toast.LENGTH_SHORT).show()
                        }
                        uiState = uiState.copy(isRecording = false)
                        recording = null
                    }
                }
            }
    }
}