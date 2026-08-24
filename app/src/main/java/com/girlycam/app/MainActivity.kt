package com.girlycam.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            setContent {
                GirlyCamApp(hasCameraPermission = granted)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            GirlyCamApp(hasCameraPermission = granted)
        }
    }

    @Composable
    private fun GirlyCamApp(hasCameraPermission: Boolean) {
        val context = LocalContext.current
        var previewView by remember { mutableStateOf<PreviewView?>(null) }
        var lastPhotoUri by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(hasCameraPermission, cameraSelector) {
            if (!hasCameraPermission) return@LaunchedEffect

            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build()
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageCapture = capture

            previewView?.let { view ->
                preview.setSurfaceProvider(view.surfaceProvider)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@MainActivity,
                    cameraSelector,
                    preview,
                    capture
                )
            }
        }

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFFF9FC)
            ) {
                if (hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { PreviewView(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 92.dp, bottom = 126.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            update = { previewView = it }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "GirlyCam",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF5B4852)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 22.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.92f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery",
                                        tint = Color(0xFF8E6878)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        takePhoto { uri ->
                                            lastPhotoUri = uri
                                        }
                                    },
                                    modifier = Modifier
                                        .size(82.dp)
                                        .background(
                                            Color(0xFFFFC7DA),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Take photo",
                                        modifier = Modifier.size(38.dp),
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        cameraSelector =
                                            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                                CameraSelector.DEFAULT_FRONT_CAMERA
                                            } else {
                                                CameraSelector.DEFAULT_BACK_CAMERA
                                            }
                                    },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.92f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.FlipCameraAndroid,
                                        contentDescription = "Flip camera",
                                        tint = Color(0xFF8E6878)
                                    )
                                }
                            }

                            if (lastPhotoUri != null) {
                                Text(
                                    text = "Saved to your gallery ♡",
                                    color = Color(0xFF8E6878),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Camera permission needed",
                            color = Color(0xFF5B4852)
                        )
                    }
                }
            }
        }
    }

    private fun takePhoto(onSaved: (String) -> Unit) {
        val capture = imageCapture ?: return

        val resolver = contentResolver
        val name = "GirlyCam_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/GirlyCam"
            )
        }

        val output = ImageCapture.OutputFileOptions.Builder(
            resolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        capture.takePicture(
            output,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    outputFileResults.savedUri?.let {
                        onSaved(it.toString())
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }
}
