package cold.nitrogen.secretcamera.ui.navigation

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cold.nitrogen.secretcamera.ui.screen.CameraScreen
import cold.nitrogen.secretcamera.ui.screen.SettingsScreen
import cold.nitrogen.secretcamera.ui.viewmodel.CameraViewModel

object Routes {
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
}

@Composable
fun CameraNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.CAMERA) {
        composable(Routes.CAMERA) {
            CameraScreen(navController)

        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController)
        }
    }
}