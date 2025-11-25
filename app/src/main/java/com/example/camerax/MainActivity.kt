package com.example.camerax

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camerax.ui.theme.CameraXTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.text.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraXTheme {
                var previewView by remember { mutableStateOf<PreviewView?>(null) }
                var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

                var camera by remember { mutableStateOf<Camera?>(null) }
                var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

                var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

                val context = this@MainActivity
                val lifecycleOwner = this@MainActivity

                var hasPermission by remember { mutableStateOf(false) }
                val cameraPermission = android.Manifest.permission.CAMERA
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasPermission = granted
                    if (!granted) {
                        Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(Unit) {
                    launcher.launch(cameraPermission)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    if (hasPermission) {
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            CameraPreview(
                                onPreviewReady = { view ->
                                    previewView = view
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            LaunchedEffect(previewView, cameraSelector) {
                                previewView?.let { view ->
                                    val provider = ProcessCameraProvider.getInstance(context).get()
                                    provider.unbindAll()

                                    val (preview, cam) = bindPreview(context, lifecycleOwner, view, cameraSelector)
                                    camera = cam
                                    imageCapture = bindWithImageCapture(provider, lifecycleOwner, preview, cameraSelector)

                                    // Reset flash ke OFF jika pindah ke kamera depan
                                    if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                        flashMode = ImageCapture.FLASH_MODE_OFF
                                    }
                                }
                            }

                            LaunchedEffect(flashMode, camera) {
                                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    try {
                                        imageCapture?.flashMode = flashMode

                                        when (flashMode) {
                                            ImageCapture.FLASH_MODE_ON -> {
                                                camera?.cameraControl?.enableTorch(true)
                                            }
                                            ImageCapture.FLASH_MODE_AUTO -> {
                                                camera?.cameraControl?.enableTorch(false)
                                            }
                                            ImageCapture.FLASH_MODE_OFF -> {
                                                camera?.cameraControl?.enableTorch(false)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(bottom = 30.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        flashMode = when (flashMode) {
                                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                            else -> ImageCapture.FLASH_MODE_OFF
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (flashMode) {
                                            ImageCapture.FLASH_MODE_ON -> Color.Yellow
                                            ImageCapture.FLASH_MODE_AUTO -> Color.White // Putih untuk Auto
                                            else -> Color.Gray
                                        },
                                        contentColor = Color.Black
                                    ),
                                    enabled = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                                ) {
                                    val text = when (flashMode) {
                                        ImageCapture.FLASH_MODE_ON -> "ON"
                                        ImageCapture.FLASH_MODE_AUTO -> "AUTO"
                                        else -> "OFF"
                                    }
                                    Text(text)
                                }

                                // Tombol Foto
                                Button(
                                    onClick = {
                                        imageCapture?.let { ic ->
                                            takePhoto(context, ic) { uri -> /* handled in helper */ }
                                        }
                                    }
                                ) {
                                    Text("FOTO")
                                }

                                // Tombol Switch
                                Button(onClick = {
                                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }
                                }) {
                                    Text("CAM")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Menunggu izin kamera...",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun CameraPreview(
    onPreviewReady: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { c -> PreviewView(c).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            post { onPreviewReady(this) }
        }},
        modifier = modifier
    )
}

suspend fun bindPreview(
    context: Context,
    owner: LifecycleOwner,
    view: PreviewView,
    selector: CameraSelector
): Pair<Preview, Camera> {
    val provider =
        suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            val f = ProcessCameraProvider.getInstance(context)
            f.addListener({ cont.resume(f.get()) {} },
                ContextCompat.getMainExecutor(context))
        }
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(view.surfaceProvider)
    }

    val camera = provider.bindToLifecycle(owner, selector, preview)
    return preview to camera
}

fun bindWithImageCapture(
    provider: ProcessCameraProvider,
    owner: LifecycleOwner,
    preview: Preview,
    selector: CameraSelector
): ImageCapture {
    val ic = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .build()
    provider.bindToLifecycle(owner, selector, preview, ic)
    return ic
}

fun outputOptions(ctx: Context, name: String): ImageCapture.OutputFileOptions {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
        }
    }

    return ImageCapture.OutputFileOptions.Builder(
        ctx.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()
}

fun takePhoto(ctx: Context, ic: ImageCapture, onSaved: (Uri?) -> Unit) {
    val name = "IMG_${System.currentTimeMillis()}"
    val options = outputOptions(ctx, name)

    ic.takePicture(
        options,
        ContextCompat.getMainExecutor(ctx),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                if (savedUri != null) {
                    Toast.makeText(ctx, "Foto tersimpan: $savedUri", Toast.LENGTH_SHORT).show()
                    onSaved(savedUri)
                } else {
                    Toast.makeText(ctx, "Foto tersimpan tapi URI null", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                Toast.makeText(ctx, "Gagal: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
