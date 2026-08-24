package com.girlycam.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private enum class FrameStyle(
    val title: String,
    val bg: Color,
    val border: Color
) {
    AESTHETIC("Aesthetic", Color(0xFFEDE4F3), Color.White),
    CLEAN("Clean", Color.White, Color(0xFFEADFE4)),
    BLUSH("Blush", Color(0xFFFFE4EE), Color(0xFFFFB8D0)),
    LAVENDER("Lavender", Color(0xFFF0E8FF), Color(0xFFD3B9FF)),
    POLAROID("Polaroid", Color.White, Color(0xFFE9E2E5)),
    FILM("Film", Color(0xFF302A31), Color(0xFF6F626D)),
    CUTE("Cute", Color(0xFFFFF1F7), Color(0xFFFF9FC1))
}

private enum class PhotoRatio(val title: String, val value: Float) {
    FOUR_FIVE("4:5", 4f / 5f),
    ONE_ONE("1:1", 1f),
    THREE_FOUR("3:4", 3f / 4f)
}

private enum class PhotoFilter(
    val title: String,
    val tint: Color
) {
    ORIGINAL("Original", Color.Transparent),
    DREAMY("Dreamy", Color(0x26FFF0F7)),
    BLUSH("Blush", Color(0x30FFB6CF)),
    SUNLIT("Sunlit", Color(0x30FFD36A)),
    WARM_BLOOM("Warm Bloom", Color(0x28FFB07A)),
    FLASH_POP("Flash Pop", Color(0x24FFFFFF)),
    RED_LIGHT("Red Light", Color(0x35FF334F)),
    GREEN_LIGHT("Green Light", Color(0x303CCB76)),
    COOL("Cool", Color(0x243FA8FF)),
    LAVENDER("Lavender", Color(0x28B894FF)),
    Y2K("Y2K", Color(0x248BD9FF)),
    NINETY("90s", Color(0x24D99B5B)),
    VHS("VHS", Color(0x245C4DFF)),
    NOKIA("Nokia", Color(0x243F7D4C)),
    CHROME("Chrome", Color(0x2430B7C8)),
    B_AND_W("B&W", Color(0x00000000)),
    FILM("Film", Color(0x1C8B6F55)),
    NIGHT_FLASH("Night Flash", Color(0x283D4DFF))
}

private enum class Sticker(val title: String, val glyph: String) {
    NONE("None", ""), BOW("Bow", "🎀"), HEART("Heart", "♡"), SPARKLE("Sparkle", "✦"),
    FLOWER("Flower", "✿"), STAR("Star", "★"), CLOUD("Cloud", "☁")
}

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            setContent { GirlyCamApp(hasCameraPermission = granted) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent { GirlyCamApp(hasCameraPermission = granted) }

        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @Composable
    private fun GirlyCamApp(hasCameraPermission: Boolean) {
        var cameraFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
        var photoUri by remember { mutableStateOf<Uri?>(null) }
        var isEditor by remember { mutableStateOf(false) }
        var collageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var isCollage by remember { mutableStateOf(false) }

        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                photoUri = uri
                isEditor = true
            }
        }

        val collageLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isNotEmpty()) {
                collageUris = uris.take(4)
                isCollage = true
            }
        }

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFFF9FC)
            ) {
                if (isCollage) {
                    CollageScreen(
                        uris = collageUris,
                        onBack = { isCollage = false; collageUris = emptyList() },
                        onPickMore = { collageLauncher.launch(arrayOf("image/*")) },
                        onSave = { saveCollage(collageUris) }
                    )
                } else if (isEditor && photoUri != null) {
                    EditorScreen(
                        uri = photoUri!!,
                        onBack = {
                            isEditor = false
                            photoUri = null
                        },
                        onRetake = {
                            isEditor = false
                            photoUri = null
                        },
                        onGallery = {
                            galleryLauncher.launch("image/*")
                        }
                    )
                } else {
                    CameraScreen(
                        hasCameraPermission = hasCameraPermission,
                        cameraFacing = cameraFacing,
                        onFlip = {
                            cameraFacing =
                                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                        },
                        onGallery = {
                            galleryLauncher.launch("image/*")
                        },
                        onCollage = { collageLauncher.launch(arrayOf("image/*")) },
                        onCaptured = { uri ->
                            photoUri = uri
                            isEditor = true
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraScreen(
        hasCameraPermission: Boolean,
        cameraFacing: Int,
        onFlip: () -> Unit,
        onGallery: () -> Unit,
        onCollage: () -> Unit,
        onCaptured: (Uri) -> Unit
    ) {
        val context = this@MainActivity
        val previewView = remember { PreviewView(context) }
        var busy by remember { mutableStateOf(false) }

        LaunchedEffect(hasCameraPermission, cameraFacing) {
            if (!hasCameraPermission) return@LaunchedEffect

            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    val provider = future.get()
                    val preview = Preview.Builder().build()
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setJpegQuality(100)
                        .build()

                    imageCapture = capture
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    provider.unbindAll()

                    provider.bindToLifecycle(
                        this@MainActivity,
                        CameraSelector.Builder()
                            .requireLensFacing(cameraFacing)
                            .build(),
                        preview,
                        capture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        if (!hasCameraPermission) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Allow camera")
                }
            }
            return
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 78.dp)
                    .clip(RoundedCornerShape(30.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                Text(
                    "GirlyCam",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF5D4A54)
                )
                Text(
                    "capture a little moment ♡",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9B7D89)
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onGallery,
                        modifier = Modifier
                            .size(58.dp)
                            .background(Color.White.copy(alpha = .94f), CircleShape)
                            .border(1.dp, Color(0xFFF0DCE5), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color(0xFF8F6878)
                        )
                    }

                    IconButton(
                        onClick = onCollage,
                        modifier = Modifier
                            .size(58.dp)
                            .background(Color.White.copy(alpha = .94f), CircleShape)
                            .border(1.dp, Color(0xFFF0DCE5), CircleShape)
                    ) {
                        Text("2×", color = Color(0xFF8F6878))
                    }

                    IconButton(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            capturePhoto { uri ->
                                busy = false
                                if (uri != null) onCaptured(uri)
                            }
                        },
                        modifier = Modifier
                            .size(84.dp)
                            .background(Color(0xFFFFBFD5), CircleShape)
                            .border(5.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Take photo",
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onFlip,
                        modifier = Modifier
                            .size(58.dp)
                            .background(Color.White.copy(alpha = .94f), CircleShape)
                            .border(1.dp, Color(0xFFF0DCE5), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch camera",
                            tint = Color(0xFF8F6878)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EditorScreen(
        uri: Uri,
        onBack: () -> Unit,
        onRetake: () -> Unit,
        onGallery: () -> Unit
    ) {
        var frame by remember { mutableStateOf(FrameStyle.AESTHETIC) }
        var ratio by remember { mutableStateOf(PhotoRatio.FOUR_FIVE) }
        var filter by remember { mutableStateOf(PhotoFilter.ORIGINAL) }
        var beauty by remember { mutableStateOf(true) }
        var grain by remember { mutableStateOf(false) }
        var sticker by remember { mutableStateOf(Sticker.NONE) }
        var saved by remember { mutableStateOf(false) }
        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF9FC))
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Make it cute",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF5D4A54)
                    )
                    Text(
                        "frames • ratios • save",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9B7D89)
                    )
                }
                IconButton(onClick = onGallery) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Choose another photo")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                PhotoFramePreview(
                    uri = uri,
                    frame = frame,
                    ratio = ratio,
                    filter = filter,
                    beauty = beauty,
                    grain = grain,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                "Frames",
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5D4A54)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrameStyle.values().forEach { item ->
                    FrameChip(
                        item = item,
                        selected = item == frame,
                        onClick = { frame = item }
                    )
                }
            }

            Text(
                "Filters",
                modifier = Modifier.padding(start = 20.dp, top = 2.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5D4A54)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PhotoFilter.values().forEach { item ->
                    FilterChip(
                        item = item,
                        selected = item == filter,
                        onClick = { filter = item }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { beauty = !beauty },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (beauty) Color(0xFFFFE2EC) else Color.Transparent,
                        contentColor = Color(0xFF795D6A)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (beauty) "Beauty ON" else "Beauty")
                }

                OutlinedButton(
                    onClick = { grain = !grain },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (grain) Color(0xFFEDE5EF) else Color.Transparent,
                        contentColor = Color(0xFF795D6A)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (grain) "Grain ON" else "Grain")
                }
            }

            Text(
                "Stickers",
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5D4A54)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Sticker.values().forEach { item ->
                    OutlinedButton(
                        onClick = { sticker = item },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (item == sticker) Color(0xFFFFE0EA) else Color.Transparent,
                            contentColor = Color(0xFF795D6A)
                        )
                    ) {
                        Text(if (item == Sticker.NONE) "None" else item.glyph)
                    }
                }
            }

            Text(
                "Ratio",
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5D4A54)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhotoRatio.values().forEach { item ->
                    OutlinedButton(
                        onClick = { ratio = item },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (item == ratio) Color(0xFFFFE0EA) else Color.Transparent,
                            contentColor = Color(0xFF795D6A)
                        )
                    ) {
                        Text(item.title)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Retake")
                }

                Button(
                    onClick = {
                        saveFramedPhoto(uri, frame, ratio, filter, beauty, grain, sticker)
                        saved = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFAFC9),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (saved) "Saved ♡" else "Save")
                }
            }
        }
    }

    @Composable
    private fun FrameChip(
        item: FrameStyle,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .width(88.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(item.bg)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Color(0xFFFF8FB3) else item.border,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .background(item.bg, RoundedCornerShape(10.dp))
                    .border(1.dp, item.border, RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(5.dp))
            Text(item.title, color = Color(0xFF715966))
        }
    }

    @Composable
    private fun FilterChip(
        item: PhotoFilter,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .width(92.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) Color(0xFFFFE0EA) else Color.White
                )
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) Color(0xFFFF8FB3) else Color(0xFFEADFE4),
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(item.tint.takeIf { it != Color.Transparent } ?: Color(0xFFFFEEF4), CircleShape)
            )
            Spacer(Modifier.height(5.dp))
            Text(item.title, color = Color(0xFF715966))
        }
    }

    @Composable
    private fun PhotoFramePreview(
        uri: Uri,
        frame: FrameStyle,
        ratio: PhotoRatio,
        filter: PhotoFilter,
        beauty: Boolean,
        grain: Boolean,
        modifier: Modifier = Modifier
    ) {
        val padding = when (frame) {
            FrameStyle.AESTHETIC -> 28.dp
            FrameStyle.POLAROID -> 22.dp
            FrameStyle.FILM -> 10.dp
            else -> 12.dp
        }

        Box(
            modifier = modifier
                .aspectRatio(ratio.value)
                .clip(RoundedCornerShape(22.dp))
                .background(frame.bg)
                .padding(padding)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Selected photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(if (frame == FrameStyle.FILM) 4.dp else 16.dp)),
                contentScale = ContentScale.Crop
            )

            if (filter.tint != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(filter.tint)
                )
            }

            if (filter == PhotoFilter.NINETY || filter == PhotoFilter.FILM || grain) {
                Text(
                    "·  ·  ˚  ·  ·  ˚  ·",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    color = Color.White.copy(alpha = .55f)
                )
            }

            if (beauty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = .07f))
                )
            }

            when (frame) {
                FrameStyle.AESTHETIC -> {
                    Text(
                        "✦   ˚₊‧  ♡  ‧₊˚   ✦",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        color = Color.White
                    )
                    Text(
                        "little memories ♡",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        color = Color(0xFF806A84)
                    )
                }
                FrameStyle.CUTE -> {
                    Text(
                        "♡  ♡",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        color = Color(0xFFFF77A4),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "˚₊‧  little moment  ‧₊˚",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        color = Color(0xFF9D667C)
                    )
                }
                FrameStyle.POLAROID -> {
                    Text(
                        "a little memory ♡",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp),
                        color = Color(0xFF806D76)
                    )
                }
                FrameStyle.FILM -> {
                    Text(
                        "GIRLYCAM  •  01",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        color = Color.White
                    )
                }
                else -> Unit
            }
        }
    }

    private fun capturePhoto(onDone: (Uri?) -> Unit) {
        val capture = imageCapture ?: run {
            onDone(null)
            return
        }

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
            contentResolver,
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
                    onDone(outputFileResults.savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    onDone(null)
                }
            }
        )
    }

    private fun saveFramedPhoto(
        uri: Uri,
        frame: FrameStyle,
        ratio: PhotoRatio,
        filter: PhotoFilter,
        beauty: Boolean,
        grain: Boolean,
        sticker: Sticker
    ) {
        val source = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return

        val width = 1080
        val height = when (ratio) {
            PhotoRatio.ONE_ONE -> 1080
            PhotoRatio.FOUR_FIVE -> 1350
            PhotoRatio.THREE_FOUR -> 1440
        }

        val output = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        canvas.drawColor(AndroidColor.parseColor(frameHex(frame.bg)))

        val margin = when (frame) {
            FrameStyle.AESTHETIC -> 88
            FrameStyle.POLAROID -> 72
            FrameStyle.FILM -> 34
            else -> 48
        }

        val bottomExtra = when (frame) {
            FrameStyle.AESTHETIC, FrameStyle.POLAROID -> 150
            else -> 48
        }
        val dst = Rect(
            margin,
            margin,
            width - margin,
            height - bottomExtra
        )

        if (frame == FrameStyle.AESTHETIC) {
            drawSparkles(canvas, width, height)
        }

        var processed = applyPhotoLook(source, filter, beauty)
        if (beauty) processed = beautifyDetectedFaces(processed)
        drawCenterCrop(canvas, processed, dst)

        if (grain || filter == PhotoFilter.NINETY || filter == PhotoFilter.FILM || filter == PhotoFilter.NOKIA || filter == PhotoFilter.VHS) {
            drawGrain(canvas, dst, if (filter == PhotoFilter.NOKIA) 0.18f else 0.10f)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = when (frame) {
                FrameStyle.FILM -> 18f
                else -> 10f
            }
            color = AndroidColor.parseColor(frameHex(frame.border))
        }
        canvas.drawRect(
            dst.left.toFloat(),
            dst.top.toFloat(),
            dst.right.toFloat(),
            dst.bottom.toFloat(),
            borderPaint
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (frame == FrameStyle.FILM) AndroidColor.WHITE
            else AndroidColor.parseColor("#9D667C")
            textSize = if (frame == FrameStyle.POLAROID) 38f else 28f
            textAlign = Paint.Align.CENTER
        }

        when (frame) {
            FrameStyle.AESTHETIC -> {
                canvas.drawText("✦  little memories  ♡", width / 2f, height - 52f, textPaint)
            }
            FrameStyle.POLAROID ->
                canvas.drawText(
                    "a little memory ♡",
                    width / 2f,
                    height - 52f,
                    textPaint
                )
            FrameStyle.CUTE -> {
                canvas.drawText("♡  ♡", width - 120f, 74f, textPaint)
                canvas.drawText(
                    "˚₊‧  little moment  ‧₊˚",
                    width / 2f,
                    height - 28f,
                    textPaint
                )
            }
            FrameStyle.FILM ->
                canvas.drawText(
                    "GIRLYCAM  •  01",
                    150f,
                    height - 22f,
                    textPaint
                )
            else -> Unit
        }

        val name = "GirlyCam_Edit_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/GirlyCam"
            )
        }

        val outUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return

        contentResolver.openOutputStream(outUri)?.use { stream ->
            output.compress(Bitmap.CompressFormat.JPEG, 94, stream)
        }
        if (sticker != Sticker.NONE) {
            val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#D85C7E")
                textSize = 72f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(sticker.glyph, width - 42f, 92f, stickerPaint)
        }

        source.recycle()
        if (processed !== source) processed.recycle()
        output.recycle()
    }

    private fun applyPhotoLook(
        source: Bitmap,
        filter: PhotoFilter,
        beauty: Boolean
    ): Bitmap {
        val output = Bitmap.createBitmap(
            source.width,
            source.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        val matrix = ColorMatrix()

        when (filter) {
            PhotoFilter.ORIGINAL -> Unit
            PhotoFilter.DREAMY -> {
                matrix.setSaturation(0.92f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.03f, 0f, 0f, 0f, 5f,
                    0f, 1.00f, 0f, 0f, 2f,
                    0f, 0f, 1.03f, 0f, 7f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            PhotoFilter.BLUSH -> {
                matrix.setSaturation(1.08f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.03f, 0f, 0f, 0f, 6f,
                    0f, 0.97f, 0f, 0f, 0f,
                    0f, 0f, 0.98f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            PhotoFilter.SUNLIT, PhotoFilter.WARM_BLOOM -> {
                matrix.setSaturation(1.08f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.06f, 0f, 0f, 0f, 10f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.90f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            PhotoFilter.FLASH_POP -> {
                matrix.setSaturation(1.15f)
                matrix.postConcat(ColorMatrix(floatArrayOf(1.10f,0f,0f,0f,8f,0f,1.10f,0f,0f,8f,0f,0f,1.10f,0f,8f,0f,0f,0f,1f,0f)))
            }
            PhotoFilter.RED_LIGHT -> matrix.postConcat(ColorMatrix(floatArrayOf(1.12f,0f,0f,0f,14f,0f,.90f,0f,0f,0f,0f,0f,.90f,0f,0f,0f,0f,0f,1f,0f)))
            PhotoFilter.GREEN_LIGHT -> matrix.postConcat(ColorMatrix(floatArrayOf(.90f,0f,0f,0f,0f,0f,1.08f,0f,0f,6f,0f,0f,.94f,0f,0f,0f,0f,0f,1f,0f)))
            PhotoFilter.LAVENDER -> matrix.postConcat(ColorMatrix(floatArrayOf(1.02f,0f,0f,0f,4f,0f,.98f,0f,0f,0f,0f,0f,1.06f,0f,8f,0f,0f,0f,1f,0f)))
            PhotoFilter.Y2K -> { matrix.setSaturation(1.22f) }
            PhotoFilter.VHS -> { matrix.setSaturation(.72f) }
            PhotoFilter.CHROME -> { matrix.setSaturation(1.28f) }
            PhotoFilter.NIGHT_FLASH -> { matrix.setSaturation(.82f); matrix.postConcat(ColorMatrix(floatArrayOf(.90f,0f,0f,0f,0f,0f,.96f,0f,0f,0f,0f,0f,1.12f,0f,10f,0f,0f,0f,1f,0f))) }
            PhotoFilter.COOL -> {
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    0.98f, 0f, 0f, 0f, 0f,
                    0f, 1.00f, 0f, 0f, 1f,
                    0f, 0f, 1.08f, 0f, 7f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            PhotoFilter.NINETY -> {
                matrix.setSaturation(0.45f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 12f,
                    0f, 0.96f, 0f, 0f, 4f,
                    0f, 0f, 0.78f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            PhotoFilter.NOKIA -> {
                matrix.setSaturation(0.30f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    0.75f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 8f,
                    0f, 0f, 0.80f, 0f, 5f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            PhotoFilter.B_AND_W -> matrix.setSaturation(0f)
            PhotoFilter.FILM -> {
                matrix.setSaturation(0.78f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.06f, 0f, 0f, 0f, 4f,
                    0f, 1.00f, 0f, 0f, 2f,
                    0f, 0f, 0.90f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
        }

        if (beauty) {
            matrix.postConcat(ColorMatrix(floatArrayOf(
                1.02f, 0f, 0f, 0f, 5f,
                0f, 1.02f, 0f, 0f, 5f,
                0f, 0f, 1.02f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            )))
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        if (beauty) {
            val softPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb(18, 255, 245, 250)
            }
            canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), softPaint)
        }

        if (filter == PhotoFilter.NOKIA) {
            val smallW = max(1, output.width / 5)
            val smallH = max(1, output.height / 5)
            val small = Bitmap.createScaledBitmap(output, smallW, smallH, false)
            val pixel = Bitmap.createScaledBitmap(small, output.width, output.height, false)
            canvas.drawBitmap(pixel, 0f, 0f, Paint())
            small.recycle()
            pixel.recycle()
        }

        return output
    }

    private fun beautifyDetectedFaces(source: Bitmap): Bitmap {
        return try {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
            val detector = FaceDetection.getClient(options)
            val image = InputImage.fromBitmap(source, 0)
            val faces = Tasks.await(detector.process(image))
            val out = source.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            for (face in faces) {
                val b = face.boundingBox
                val left = b.left.coerceAtLeast(0)
                val top = b.top.coerceAtLeast(0)
                val right = b.right.coerceAtMost(source.width)
                val bottom = b.bottom.coerceAtMost(source.height)
                if (right <= left || bottom <= top) continue
                val crop = Bitmap.createBitmap(source, left, top, right-left, bottom-top)
                val small = Bitmap.createScaledBitmap(crop, max(1,(crop.width/7)), max(1,(crop.height/7)), true)
                val smooth = Bitmap.createScaledBitmap(small, crop.width, crop.height, true)
                canvas.drawBitmap(smooth, left.toFloat(), top.toFloat(), paint)
                crop.recycle(); small.recycle(); smooth.recycle()
            }
            detector.close()
            out
        } catch (_: Exception) { source }
    }

    private fun drawGrain(canvas: Canvas, dst: Rect, alpha: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val random = java.util.Random(42L)
        val count = (dst.width() * dst.height() / 1200).coerceAtMost(12000)
        for (i in 0 until count) {
            val x = dst.left + random.nextInt(max(1, dst.width()))
            val y = dst.top + random.nextInt(max(1, dst.height()))
            val light = random.nextBoolean()
            paint.color = if (light) {
                AndroidColor.argb((255 * alpha).toInt(), 255, 255, 255)
            } else {
                AndroidColor.argb((255 * alpha).toInt(), 30, 20, 30)
            }
            canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
        }
    }

    private fun drawSparkles(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(170, 255, 255, 255)
        }
        val points = listOf(
            width * .12f to height * .18f,
            width * .88f to height * .22f,
            width * .17f to height * .74f,
            width * .82f to height * .68f,
            width * .48f to height * .10f
        )
        points.forEach { (x, y) ->
            canvas.drawCircle(x, y, 3.5f, paint)
            canvas.drawRect(x - 1.2f, y - 18f, x + 1.2f, y + 18f, paint)
            canvas.drawRect(x - 18f, y - 1.2f, x + 18f, y + 1.2f, paint)
        }
    }

    private fun drawCenterCrop(
        canvas: Canvas,
        bitmap: Bitmap,
        dst: Rect
    ) {
        val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val dstRatio = dst.width().toFloat() / dst.height().toFloat()

        val src = if (srcRatio > dstRatio) {
            val newWidth = (bitmap.height * dstRatio).toInt()
            val left = (bitmap.width - newWidth) / 2
            Rect(left, 0, left + newWidth, bitmap.height)
        } else {
            val newHeight = (bitmap.width / dstRatio).toInt()
            val top = (bitmap.height - newHeight) / 2
            Rect(0, top, bitmap.width, top + newHeight)
        }

        canvas.drawBitmap(bitmap, src, dst, Paint(Paint.ANTI_ALIAS_FLAG))
    }

    private fun frameHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
    }
    @Composable
    private fun CollageScreen(
        uris: List<Uri>,
        onBack: () -> Unit,
        onPickMore: () -> Unit,
        onSave: () -> Unit
    ) {
        var layout by remember { mutableIntStateOf(uris.size.coerceIn(2, 4)) }
        Column(Modifier.fillMaxSize().background(Color(0xFFFFF9FC)).padding(18.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                Text("Cute Collage", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), color = Color(0xFF5D4A54))
                Button(onClick = onSave) { Text("Save") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2,3,4).forEach { n ->
                    OutlinedButton(onClick = { layout = n }, colors = ButtonDefaults.outlinedButtonColors(containerColor = if(layout==n) Color(0xFFFFE0EA) else Color.Transparent)) { Text("$n photos") }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp)).background(Color(0xFFF0E8FF)).padding(14.dp)) {
                val selected = uris.take(layout)
                if (selected.isEmpty()) {
                    Text("Pick 2–4 photos", Modifier.align(Alignment.Center), color = Color(0xFF9B7D89))
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (layout) {
                            2 -> {
                                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { selected.forEach { AsyncImage(it, null, Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) } }
                            }
                            3 -> {
                                AsyncImage(selected[0], null, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) { selected.drop(1).forEach { AsyncImage(it, null, Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) } }
                            }
                            else -> {
                                selected.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { AsyncImage(it, null, Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) } } }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onPickMore, modifier = Modifier.fillMaxWidth()) { Text("Choose photos") }
        }
    }

    private fun saveCollage(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val size = 1200
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(AndroidColor.parseColor("#F0E8FF"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val gap = 24
        val cell = (size-gap*3)/2
        uris.take(4).forEachIndexed { i, uri ->
            val src = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@forEachIndexed
            val row=i/2; val col=i%2
            val left=gap+col*(cell+gap); val top=gap+row*(cell+gap)
            drawCenterCrop(canvas, src, Rect(left, top, left+cell, top+cell)); src.recycle()
        }
        val name="GirlyCam_Collage_${System.currentTimeMillis()}.jpg"
        val values=ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,name); put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg"); put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/GirlyCam") }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { out -> contentResolver.openOutputStream(out)?.use { bmp.compress(Bitmap.CompressFormat.JPEG,94,it) } }
        bmp.recycle()
    }

}
