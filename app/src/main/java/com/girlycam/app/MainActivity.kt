package com.girlycam.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private enum class AppPage { HOME, CAMERA, EDITOR, COLLAGE }

private enum class PhotoFilter(val title: String) {
    Original("Original"),
    Soft("Soft"),
    Glow("Glow"),
    Pink("Pink"),
    Warm("Sunlight"),
    Cool("Cool"),
    Vintage("90s"),
    Nokia("Nokia"),
    Mono("B&W"),
    Red("Red Light"),
    Green("Green Light"),
    Flash("Flash"),
    Grain("Grain")
}

private enum class FrameStyle(val title: String) {
    None("None"),
    White("White"),
    Pink("Pink"),
    Polaroid("Polaroid"),
    Doodle("Cute")
}

private val cuteStickers = listOf("♡", "✦", "🌸", "🎀", "☁", "★")

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            setContent { GirlyCamApp(granted) }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { openEditor(it) }
        }

    private val collageLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.size >= 2) {
                collageUris = uris.take(4)
                currentPage = AppPage.COLLAGE
                render()
            }
        }

    private var currentPage by mutableStateOf(AppPage.HOME)
    private var editorUri by mutableStateOf<Uri?>(null)
    private var collageUris by mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent { GirlyCamApp(granted) }
    }

    private fun render() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        setContent { GirlyCamApp(granted) }
    }

    private fun openEditor(uri: Uri) {
        editorUri = uri
        currentPage = AppPage.EDITOR
        render()
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return

        val name = "GirlyCam_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GirlyCam")
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
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    result.savedUri?.let { openEditor(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }

    @Composable
    private fun GirlyCamApp(hasCameraPermission: Boolean) {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFFF8FC)
            ) {
                when (currentPage) {
                    AppPage.HOME -> HomeScreen()
                    AppPage.CAMERA -> CameraScreen(hasCameraPermission)
                    AppPage.EDITOR -> EditorScreen(editorUri)
                    AppPage.COLLAGE -> CollageScreen(collageUris)
                }
            }
        }
    }

    @Composable
    private fun HomeScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(34.dp))
            Text(
                "GirlyCam",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B4A5B)
            )
            Text(
                "cute camera • filters • memories",
                color = Color(0xFF9B7B8A),
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(36.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDDEA))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Make your photo cute ✦", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Shoot, edit, add filters, frames, stickers and save to Gallery.",
                        color = Color(0xFF765666),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { currentPage = AppPage.CAMERA; render() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(10.dp))
                Text("Open Camera")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(10.dp))
                Text("Choose from Gallery")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { collageLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Collections, null)
                Spacer(Modifier.width(10.dp))
                Text("Make 2 / 3 / 4 Photo Collage")
            }

            Spacer(Modifier.weight(1f))
            Text(
                "Photos are saved in Pictures/GirlyCam",
                color = Color(0xFF9B7B8A),
                fontSize = 13.sp
            )
        }
    }

    @Composable
    private fun CameraScreen(hasCameraPermission: Boolean) {
        var previewView by remember { mutableStateOf<PreviewView?>(null) }
        var flashOn by remember { mutableStateOf(false) }

        LaunchedEffect(hasCameraPermission, cameraSelector) {
            if (!hasCameraPermission) return@LaunchedEffect

            val provider = ProcessCameraProvider.getInstance(this@MainActivity).get()
            val preview = Preview.Builder().build()
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageCapture = capture

            previewView?.let { view ->
                preview.setSurfaceProvider(view.surfaceProvider)
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    this@MainActivity,
                    cameraSelector,
                    preview,
                    capture
                )
                camera?.cameraControl?.enableTorch(flashOn)
            }
        }

        LaunchedEffect(flashOn) {
            camera?.cameraControl?.enableTorch(flashOn)
        }

        if (!hasCameraPermission) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow Camera")
                }
            }
            return
        }

        Box(Modifier.fillMaxSize().background(Color(0xFF1B171A))) {
            AndroidView(
                factory = { PreviewView(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 72.dp, bottom = 160.dp)
                    .clip(RoundedCornerShape(32.dp)),
                update = { previewView = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { currentPage = AppPage.HOME; render() },
                    modifier = Modifier.background(Color.Black.copy(alpha = .45f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }

                IconButton(
                    onClick = { flashOn = !flashOn },
                    modifier = Modifier.background(Color.Black.copy(alpha = .45f), CircleShape)
                ) {
                    Icon(
                        if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        null,
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 30.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.size(58.dp).background(Color.White.copy(.92f), CircleShape)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFF8B5870))
                }

                IconButton(
                    onClick = { takePhoto() },
                    modifier = Modifier
                        .size(88.dp)
                        .background(Color(0xFFFFB8D2), CircleShape)
                        .border(5.dp, Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                IconButton(
                    onClick = {
                        cameraSelector =
                            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            else
                                CameraSelector.DEFAULT_BACK_CAMERA
                    },
                    modifier = Modifier.size(58.dp).background(Color.White.copy(.92f), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, null, tint = Color(0xFF8B5870))
                }
            }
        }
    }

    @Composable
    private fun EditorScreen(uri: Uri?) {
        if (uri == null) {
            currentPage = AppPage.HOME
            render()
            return
        }

        val context = LocalContext.current
        var original by remember(uri) { mutableStateOf<Bitmap?>(null) }
        var selectedFilter by remember { mutableStateOf(PhotoFilter.Original) }
        var beauty by remember { mutableStateOf(0.25f) }
        var frame by remember { mutableStateOf(FrameStyle.None) }
        var sticker by remember { mutableStateOf("") }
        var edited by remember { mutableStateOf<Bitmap?>(null) }
        var busy by remember { mutableStateOf(false) }

        LaunchedEffect(uri) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use {
                    original = BitmapFactory.decodeStream(it)?.let { b -> scaleBitmap(b, 1600) }
                }
            }
        }

        LaunchedEffect(original, selectedFilter, beauty, frame, sticker) {
            val source = original ?: return@LaunchedEffect
            busy = true
            edited = withContext(Dispatchers.Default) {
                makeEditedBitmap(source, selectedFilter, beauty, frame, sticker)
            }
            busy = false
        }

        Column(
            Modifier.fillMaxSize().padding(top = 12.dp).navigationBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentPage = AppPage.HOME; render() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Text("Edit Photo", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {
                        edited?.let { saveBitmapToGallery(it) }
                    },
                    enabled = edited != null && !busy
                ) {
                    Icon(Icons.Default.Save, null)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFEFE7EB)),
                contentAlignment = Alignment.Center
            ) {
                edited?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Edited photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Text("Filters", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhotoFilter.values().forEach { filter ->
                    FilterChip(filter.title, selectedFilter == filter) {
                        selectedFilter = filter
                    }
                }
            }

            Text("Beauty ${((beauty * 100).toInt())}%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Slider(
                value = beauty,
                onValueChange = { beauty = it },
                valueRange = 0f..1f,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Text("Frames", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrameStyle.values().forEach { item ->
                    FilterChip(item.title, frame == item) { frame = item }
                }
            }

            Text("Cute stickers", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip("None", sticker.isEmpty()) { sticker = "" }
                cuteStickers.forEach { s ->
                    FilterChip(s, sticker == s) { sticker = s }
                }
            }
        }
    }

    @Composable
    private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
        Text(
            text = text,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) Color(0xFFFFB8D2) else Color.White)
                .border(
                    1.dp,
                    if (selected) Color(0xFFFF7FAE) else Color(0xFFE8D8E0),
                    RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            color = Color(0xFF624B57)
        )
    }

    @Composable
    private fun CollageScreen(uris: List<Uri>) {
        val context = LocalContext.current
        var bitmaps by remember(uris) { mutableStateOf<List<Bitmap>>(emptyList()) }
        var layout by remember { mutableStateOf(2) }

        LaunchedEffect(uris) {
            bitmaps = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)?.let { b -> scaleBitmap(b, 1000) }
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(14.dp).navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentPage = AppPage.HOME; render() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Text("Collage", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {
                        if (bitmaps.size >= 2) saveBitmapToGallery(makeCollage(bitmaps, layout))
                    }
                ) {
                    Icon(Icons.Default.Save, null)
                }
            }

            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFEFE7EB)),
                contentAlignment = Alignment.Center
            ) {
                val collage = remember(bitmaps, layout) {
                    if (bitmaps.size >= 2) makeCollage(bitmaps, layout) else null
                }
                collage?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Collage preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Text("Layout", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 3, 4).forEach { count ->
                    FilterChip("$count photos", layout == count) {
                        layout = count.coerceAtMost(max(2, bitmaps.size))
                    }
                }
            }

            Button(
                onClick = { collageLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Collections, null)
                Spacer(Modifier.width(8.dp))
                Text("Choose Photos Again")
            }
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val name = "GirlyCam_Edit_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GirlyCam")
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return

        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            e.printStackTrace()
        }
    }
}

private fun scaleBitmap(source: Bitmap, maxSide: Int): Bitmap {
    val biggest = max(source.width, source.height)
    if (biggest <= maxSide) return source
    val scale = maxSide.toFloat() / biggest.toFloat()
    return Bitmap.createScaledBitmap(
        source,
        max(1, (source.width * scale).toInt()),
        max(1, (source.height * scale).toInt()),
        true
    )
}

private fun makeEditedBitmap(
    source: Bitmap,
    filter: PhotoFilter,
    beauty: Float,
    frame: FrameStyle,
    sticker: String
): Bitmap {
    val base = source.copy(Bitmap.Config.ARGB_8888, true)
    val out = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val matrix = ColorMatrix()
    when (filter) {
        PhotoFilter.Original -> {}
        PhotoFilter.Soft -> matrix.set(floatArrayOf(
            1.04f, 0f, 0f, 0f, 4f,
            0f, 1.02f, 0f, 0f, 3f,
            0f, 0f, 1.02f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
        ))
        PhotoFilter.Glow -> matrix.set(floatArrayOf(
            1.08f, 0f, 0f, 0f, 10f,
            0f, 1.05f, 0f, 0f, 8f,
            0f, 0f, 1.08f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        ))
        PhotoFilter.Pink -> matrix.set(floatArrayOf(
            1.05f, 0f, 0f, 0f, 8f,
            0f, 0.96f, 0f, 0f, 3f,
            0f, 0f, 1.02f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        ))
        PhotoFilter.Warm -> matrix.set(floatArrayOf(
            1.10f, 0f, 0f, 0f, 12f,
            0f, 1.02f, 0f, 0f, 4f,
            0f, 0f, 0.88f, 0f, -2f,
            0f, 0f, 0f, 1f, 0f
        ))
        PhotoFilter.Cool -> matrix.set(floatArrayOf(
            0.94f, 0f, 0f, 0f, 0f,
            0f, 1.00f, 0f, 0f, 0f,
            0f, 0f, 1.10f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        ))
        PhotoFilter.Vintage -> matrix.set(floatArrayOf(
            0.92f, 0.04f, 0.02f, 0f, 8f,
            0.02f, 0.88f, 0.04f, 0f, 5f,
            0.01f, 0.05f, 0.78f, 0f, 2f,
            0f, 0f, 0f, 1f, 0f
        ))
        PhotoFilter.Nokia -> matrix.set(floatArrayOf(
            0.75f, 0.10f, 0.05f, 0f, 18f,
            0.08f, 0.86f, 0.04f, 0f, 16f,
            0.03f, 0.10f, 0.60f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        ))
        PhotoFilter.Mono -> {
            val gray = ColorMatrix()
            gray.setSaturation(0f)
            matrix.setConcat(gray, ColorMatrix(floatArrayOf(
                1.08f,0f,0f,0f,4f, 0f,1.08f,0f,0f,4f,
                0f,0f,1.08f,0f,4f, 0f,0f,0f,1f,0f
            )))
        }
        PhotoFilter.Red -> matrix.set(floatArrayOf(
            1.18f,0f,0f,0f,18f,
            0f,0.72f,0f,0f,0f,
            0f,0f,0.78f,0f,0f,
            0f,0f,0f,1f,0f
        ))
        PhotoFilter.Green -> matrix.set(floatArrayOf(
            0.82f,0f,0f,0f,0f,
            0f,1.16f,0f,0f,10f,
            0f,0f,0.84f,0f,0f,
            0f,0f,0f,1f,0f
        ))
        PhotoFilter.Flash -> matrix.set(floatArrayOf(
            1.22f,0f,0f,0f,20f,
            0f,1.18f,0f,0f,18f,
            0f,0f,1.12f,0f,14f,
            0f,0f,0f,1f,0f
        ))
        PhotoFilter.Grain -> {}
    }

    paint.colorFilter = ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(base, null, Rect(0, 0, out.width, out.height), paint)

    if (beauty > 0f) {
        val overlay = Paint(Paint.ANTI_ALIAS_FLAG)
        overlay.color = AndroidColor.WHITE
        overlay.alpha = (beauty * 28f).toInt()
        canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), overlay)
    }

    if (filter == PhotoFilter.Grain) {
        addGrain(out, 0.10f)
    }

    val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    when (frame) {
        FrameStyle.None -> {}
        FrameStyle.White -> {
            framePaint.style = Paint.Style.STROKE
            framePaint.strokeWidth = max(10f, out.width * .018f)
            framePaint.color = AndroidColor.WHITE
            canvas.drawRect(
                framePaint.strokeWidth,
                framePaint.strokeWidth,
                out.width - framePaint.strokeWidth,
                out.height - framePaint.strokeWidth,
                framePaint
            )
        }
        FrameStyle.Pink -> {
            framePaint.style = Paint.Style.STROKE
            framePaint.strokeWidth = max(12f, out.width * .022f)
            framePaint.color = AndroidColor.rgb(255, 170, 201)
            canvas.drawRect(
                framePaint.strokeWidth,
                framePaint.strokeWidth,
                out.width - framePaint.strokeWidth,
                out.height - framePaint.strokeWidth,
                framePaint
            )
        }
        FrameStyle.Polaroid -> {
            framePaint.color = AndroidColor.WHITE
            canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), framePaint)
            canvas.drawBitmap(base, null, Rect(25, 25, out.width - 25, out.height - 150), paint)
        }
        FrameStyle.Doodle -> {
            framePaint.style = Paint.Style.STROKE
            framePaint.strokeWidth = max(8f, out.width * .014f)
            framePaint.color = AndroidColor.rgb(255, 205, 225)
            canvas.drawRoundRect(
                20f, 20f, out.width - 20f, out.height - 20f,
                36f, 36f, framePaint
            )
        }
    }

    if (sticker.isNotEmpty()) {
        val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        stickerPaint.textSize = max(50f, out.width * .10f)
        stickerPaint.color = AndroidColor.WHITE
        stickerPaint.setShadowLayer(8f, 0f, 3f, AndroidColor.GRAY)
        canvas.drawText(sticker, out.width * .72f, out.height * .16f, stickerPaint)
    }

    return out
}

private fun addGrain(bitmap: Bitmap, amount: Float) {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val random = Random(System.currentTimeMillis())
    val strength = (amount * 80f).toInt()

    for (i in pixels.indices) {
        val c = pixels[i]
        val n = random.nextInt(-strength, strength + 1)
        val r = min(255, max(0, AndroidColor.red(c) + n))
        val g = min(255, max(0, AndroidColor.green(c) + n))
        val b = min(255, max(0, AndroidColor.blue(c) + n))
        pixels[i] = AndroidColor.argb(AndroidColor.alpha(c), r, g, b)
    }
    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
}

private fun makeCollage(bitmaps: List<Bitmap>, requestedCount: Int): Bitmap {
    val count = min(requestedCount, bitmaps.size).coerceIn(2, 4)
    val size = 1000
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(AndroidColor.WHITE)

    val gap = 10
    val half = (size - gap * 3) / 2
    val positions = when (count) {
        2 -> listOf(
            Rect(gap, gap, size - gap, half + gap),
            Rect(gap, half + gap * 2, size - gap, size - gap)
        )
        3 -> listOf(
            Rect(gap, gap, half + gap, half + gap),
            Rect(half + gap * 2, gap, size - gap, half + gap),
            Rect(gap, half + gap * 2, size - gap, size - gap)
        )
        else -> listOf(
            Rect(gap, gap, half + gap, half + gap),
            Rect(half + gap * 2, gap, size - gap, half + gap),
            Rect(gap, half + gap * 2, half + gap, size - gap),
            Rect(half + gap * 2, half + gap * 2, size - gap, size - gap)
        )
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    for (i in 0 until count) {
        canvas.drawBitmap(
            bitmaps[i],
            null,
            positions[i],
            paint
        )
    }
    return output
}
