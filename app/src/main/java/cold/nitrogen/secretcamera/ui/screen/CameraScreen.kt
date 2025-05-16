package cold.nitrogen.secretcamera.ui.screen

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import cold.nitrogen.secretcamera.ui.navigation.Routes
import cold.nitrogen.secretcamera.ui.screen.state.CameraMode
import cold.nitrogen.secretcamera.ui.screen.state.CameraUiState
import cold.nitrogen.secretcamera.ui.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(navController: NavController) {
    val viewModel: CameraViewModel = hiltViewModel()

    val uiState by viewModel.uiStateFlow.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val permissionsList = rememberMultiplePermissionsState(
        mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    )

    var requestedPermissions by remember { mutableStateOf(false) }

    LaunchedEffect(permissionsList.allPermissionsGranted) {
        if (!requestedPermissions) {
            permissionsList.launchMultiplePermissionRequest()
            requestedPermissions = true
        }
        if (permissionsList.allPermissionsGranted) {
            viewModel.startCamera(context, lifecycleOwner, previewView)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        previewView
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.setMode(CameraMode.PHOTO) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Photo")
                }

                Button(
                    onClick = { viewModel.setMode(CameraMode.VIDEO) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Video")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        when (uiState.mode) {
                            CameraMode.PHOTO -> viewModel.takePhoto(context)
                            CameraMode.VIDEO -> viewModel.captureVideo(context)
                        }
                    },
                    modifier = Modifier
                        .weight(3f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        when (uiState.mode) {
                            CameraMode.PHOTO -> "Take Photo"
                            CameraMode.VIDEO -> if (uiState.isRecording) "Start" else "Stop"
                        }
                    )
                }

                Button(
                    onClick = { navController.navigate(Routes.SETTINGS) },
                    modifier = Modifier
                        .weight(1f),
                    shape = CircleShape,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopCamera()
        }
    }
}
