package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BorderInteractive
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CanvasDefault
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.PrimaryCore
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.SurfaceBase
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.InputStream
import java.util.concurrent.Executors

@Composable
fun CameraScanView(
    onImageCaptured: (Bitmap) -> Unit,
    onSampleSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker for selecting existing receipts
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    onImageCaptured(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    // Animated scanning line
    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasDefault)
            .testTag("camera_scan_view")
    ) {
        // Camera Preview if permission granted
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback view when camera is not permitted or unavailable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "High-Precision Tax OCR",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant camera permission to capture physical receipts, pick an invoice from gallery, or test with instant sample data.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryCore)
                        .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .testTag("grant_camera_permission_button")
                ) {
                    Text(
                        text = "Enable Camera",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // HUD Reticle Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val frameLeft = width * 0.12f
            val frameTop = height * 0.18f
            val frameRight = width * 0.88f
            val frameBottom = height * 0.68f

            val cornerLength = 36f
            val strokeWidth = 5f
            val cornerColor = Color(0xFF38BDF8)

            // Top-Left corner
            drawLine(cornerColor, Offset(frameLeft, frameTop), Offset(frameLeft + cornerLength, frameTop), strokeWidth)
            drawLine(cornerColor, Offset(frameLeft, frameTop), Offset(frameLeft, frameTop + cornerLength), strokeWidth)

            // Top-Right corner
            drawLine(cornerColor, Offset(frameRight, frameTop), Offset(frameRight - cornerLength, frameTop), strokeWidth)
            drawLine(cornerColor, Offset(frameRight, frameTop), Offset(frameRight, frameTop + cornerLength), strokeWidth)

            // Bottom-Left corner
            drawLine(cornerColor, Offset(frameLeft, frameBottom), Offset(frameLeft + cornerLength, frameBottom), strokeWidth)
            drawLine(cornerColor, Offset(frameLeft, frameBottom), Offset(frameLeft, frameBottom - cornerLength), strokeWidth)

            // Bottom-Right corner
            drawLine(cornerColor, Offset(frameRight, frameBottom), Offset(frameRight - cornerLength, frameBottom), strokeWidth)
            drawLine(cornerColor, Offset(frameRight, frameBottom), Offset(frameRight, frameBottom - cornerLength), strokeWidth)

            // Animated Laser Scanning Line
            val currentLineY = frameTop + (frameBottom - frameTop) * scanLineProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF38BDF8).copy(alpha = 0.8f),
                        Color(0xFF6366F1),
                        Color(0xFF38BDF8).copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    startX = frameLeft,
                    endX = frameRight
                ),
                start = Offset(frameLeft, currentLineY),
                end = Offset(frameRight, currentLineY),
                strokeWidth = 4f
            )
        }

        // Top Header Cockpit HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceBase.copy(alpha = 0.85f))
                    .border(1.dp, BorderInteractive, CircleShape)
                    .testTag("close_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = TextPrimary
                )
            }

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceBase.copy(alpha = 0.85f))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GEMINI 3.8 TAX VISION",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceBase.copy(alpha = 0.85f))
                    .border(1.dp, BorderInteractive, CircleShape)
                    .testTag("flip_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Camera",
                    tint = TextPrimary
                )
            }
        }

        // Bottom Controls Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, CanvasDefault.copy(alpha = 0.95f), CanvasDefault)
                    )
                )
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sample test receipts quick-selection
            Text(
                text = "QUICK TEST PRESETS",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SampleChip(label = "AWS Hosting", onClick = { onSampleSelected(0) })
                SampleChip(label = "Client Lunch", onClick = { onSampleSelected(1) })
                SampleChip(label = "Hardware", onClick = { onSampleSelected(2) })
                SampleChip(label = "Co-Working", onClick = { onSampleSelected(3) })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Shutter & Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo Picker from Gallery
                IconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SurfaceBase)
                        .border(1.dp, BorderInteractive, CircleShape)
                        .testTag("gallery_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Pick receipt from gallery",
                        tint = TextPrimary
                    )
                }

                // Shutter Capture Button with glowing Electric Indigo ring
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(PrimaryCore.copy(alpha = 0.25f))
                        .border(2.dp, PrimaryCore, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (hasCameraPermission) {
                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            ContextCompat.getMainExecutor(context).execute {
                                                if (bitmap != null) {
                                                    onImageCaptured(bitmap)
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            // Fallback to sample on hardware failure
                                            ContextCompat.getMainExecutor(context).execute {
                                                onSampleSelected(0)
                                            }
                                        }
                                    }
                                )
                            } else {
                                onSampleSelected(0)
                            }
                        }
                        .testTag("capture_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(PrimaryCore)
                    )
                }

                // Sample test trigger
                IconButton(
                    onClick = { onSampleSelected(0) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SurfaceBase)
                        .border(1.dp, BorderInteractive, CircleShape)
                        .testTag("quick_sample_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Test Sample",
                        tint = SecondaryEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceBase.copy(alpha = 0.85f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
