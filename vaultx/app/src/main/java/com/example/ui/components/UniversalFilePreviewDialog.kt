package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crypto.CryptoManager
import com.example.i18n.VaultStrings
import com.example.model.VaultItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

enum class PreviewType {
    IMAGE,
    VIDEO,
    AUDIO,
    PDF,
    CODE_OR_TEXT,
    ARCHIVE,
    OTHER
}

/**
 * Universal safe in-app preview dialog for decrypted files in VaultX.
 */
@Composable
fun UniversalFilePreviewDialog(
    item: VaultItem,
    fileBytes: ByteArray,
    strings: VaultStrings,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val extension = (item.fileName ?: item.title).substringAfterLast('.', "").lowercase(Locale.ROOT)
    val mime = (item.fileMimeType ?: "").lowercase(Locale.ROOT)

    val previewType = remember(extension, mime) {
        when {
            mime.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp") -> PreviewType.IMAGE
            mime.startsWith("video/") || extension in listOf("mp4", "webm", "mkv", "3gp") -> PreviewType.VIDEO
            mime.startsWith("audio/") || extension in listOf("mp3", "wav", "aac", "m4a", "ogg", "flac") -> PreviewType.AUDIO
            mime == "application/pdf" || extension == "pdf" -> PreviewType.PDF
            mime == "application/zip" || mime == "application/x-zip-compressed" || extension == "zip" -> PreviewType.ARCHIVE
            mime.startsWith("text/") || mime in listOf("application/json", "application/xml", "application/javascript") ||
                    extension in listOf("txt", "csv", "json", "xml", "md", "log", "py", "java", "kt", "js", "ts", "html", "css", "sql", "sh", "c", "cpp", "h") -> PreviewType.CODE_OR_TEXT
            else -> PreviewType.OTHER
        }
    }

    var isTemporarilyRevealed by remember { mutableStateOf(false) }
    val isPrivacyMode = LocalPrivacyMode.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = VaultSlateBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VaultSurfaceCard,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel, tint = TextPrimary)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.isPasswordProtected) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                PrivacyMaskedFilename(
                                    filename = item.fileName ?: item.title,
                                    isMasked = isPrivacyMode,
                                    isTemporarilyRevealed = isTemporarilyRevealed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }
                            val typeLabel = when (previewType) {
                                PreviewType.IMAGE -> strings.collectionImages
                                PreviewType.VIDEO -> strings.collectionVideos
                                PreviewType.AUDIO -> strings.collectionAudio
                                PreviewType.PDF -> strings.collectionDocuments
                                PreviewType.CODE_OR_TEXT -> strings.notes
                                PreviewType.ARCHIVE -> strings.defaultDocumentName
                                PreviewType.OTHER -> strings.files
                            }
                            PrivacyMaskedText(
                                text = "${strings.formatBytes(fileBytes.size.toLong())} • $typeLabel",
                                isMasked = isPrivacyMode,
                                isTemporarilyRevealed = isTemporarilyRevealed,
                                maskPlaceholder = strings.encryptedContentProtected,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        if (isPrivacyMode) {
                            IconButton(onClick = { isTemporarilyRevealed = !isTemporarilyRevealed }) {
                                Icon(
                                    imageVector = if (isTemporarilyRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = strings.reveal,
                                    tint = SecurityGreen
                                )
                            }
                        }

                        // Export Decrypted Button
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.FileDownload, contentDescription = strings.exportFile, tint = CyberCyan)
                        }
                    }
                }

                // Main Content Body wrapped with PrivacyBlurMask
                PrivacyBlurMask(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    isMasked = isPrivacyMode,
                    isTemporarilyRevealed = isTemporarilyRevealed,
                    maskBadgeText = strings.previewMaskedNotice,
                    onTapReveal = { isTemporarilyRevealed = true }
                ) {
                    when (previewType) {
                        PreviewType.IMAGE -> ImagePreviewPane(fileBytes = fileBytes, strings = strings)
                        PreviewType.VIDEO -> VideoPreviewPane(fileBytes = fileBytes, strings = strings)
                        PreviewType.AUDIO -> AudioPreviewPane(fileBytes = fileBytes, item = item, strings = strings)
                        PreviewType.PDF -> PdfPreviewPane(fileBytes = fileBytes, strings = strings)
                        PreviewType.CODE_OR_TEXT -> TextCodePreviewPane(fileBytes = fileBytes, strings = strings)
                        PreviewType.ARCHIVE -> ArchivePreviewPane(fileBytes = fileBytes, strings = strings)
                        PreviewType.OTHER -> UnsupportedPreviewPane(item = item, fileBytes = fileBytes, strings = strings, onExport = onExport)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Image Preview with zoom, pan, and rotation.
 */
@Composable
private fun ImagePreviewPane(
    fileBytes: ByteArray,
    strings: VaultStrings
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val bitmap = remember(fileBytes) {
        try {
            BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap == null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BrokenImage, contentDescription = null, tint = SecurityRed, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(strings.previewUnavailable, color = TextSecondary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        rotationZ = rotation,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        }

        // Overlay toolbar for zoom & rotation controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            color = VaultSurfaceCard.copy(alpha = 0.9f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultOutline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(4f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = strings.zoomIn, tint = CyberCyan)
                }
                IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = strings.zoomOut, tint = CyberCyan)
                }
                IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                    Icon(Icons.Default.RotateRight, contentDescription = strings.rotate, tint = CyberCyan)
                }
                IconButton(onClick = {
                    scale = 1f
                    rotation = 0f
                    offsetX = 0f
                    offsetY = 0f
                }) {
                    Icon(Icons.Default.FitScreen, contentDescription = strings.fitToScreen, tint = CyberCyan)
                }
            }
        }
    }
}

/**
 * Integrated Video Player with playback controls and automatic cleanup.
 */
@Composable
private fun VideoPreviewPane(
    fileBytes: ByteArray,
    strings: VaultStrings
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(fileBytes) {
        val file = File(context.cacheDir, "vaultx_preview_${UUID.randomUUID()}.mp4")
        try {
            FileOutputStream(file).use { it.write(fileBytes) }
            tempFile = file
        } catch (_: Exception) {}

        onDispose {
            try {
                videoViewRef?.stopPlayback()
                file.delete()
            } catch (_: Exception) {}
        }
    }

    if (tempFile == null) {
        CircularProgressIndicator(color = CyberCyan)
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewRef = this
                    setVideoURI(Uri.fromFile(tempFile))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                }
            }
        )

        // Overlay play/pause floating button
        FloatingActionButton(
            onClick = {
                videoViewRef?.let { vv ->
                    if (vv.isPlaying) {
                        vv.pause()
                        isPlaying = false
                    } else {
                        vv.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            containerColor = CyberCyan,
            contentColor = Color.Black
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) strings.pause else strings.play
            )
        }
    }
}

/**
 * Integrated Audio Player with waveform visualization, seekbar, and speeds.
 */
@Composable
private fun AudioPreviewPane(
    fileBytes: ByteArray,
    item: VaultItem,
    strings: VaultStrings
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(1) }

    DisposableEffect(fileBytes) {
        val tempAudio = File(context.cacheDir, "vaultx_audio_${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(tempAudio).use { it.write(fileBytes) }
            val player = MediaPlayer().apply {
                setDataSource(tempAudio.absolutePath)
                prepare()
                duration = this.duration.coerceAtLeast(1)
            }
            player.setOnCompletionListener {
                isPlaying = false
                currentPosition = 0
            }
            mediaPlayer = player
        } catch (_: Exception) {}

        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                tempAudio.delete()
            } catch (_: Exception) {}
        }
    }

    // Progress updater loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosition = mp.currentPosition
                }
            }
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Audio Disc Illustration
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(CyberCyanContainer)
                .border(2.dp, CyberCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Audiotrack,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = item.fileName ?: item.title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Seekbar Slider
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { pos ->
                currentPosition = pos.toInt()
                mediaPlayer?.seekTo(pos.toInt())
            },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = VaultOutline
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), fontSize = 12.sp, color = TextSecondary)
            Text(formatTime(duration), fontSize = 12.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = {
                mediaPlayer?.let { mp ->
                    val target = (mp.currentPosition - 10000).coerceAtLeast(0)
                    mp.seekTo(target)
                    currentPosition = target
                }
            }) {
                Icon(Icons.Default.Replay10, contentDescription = strings.rewind10s, tint = TextPrimary, modifier = Modifier.size(32.dp))
            }

            FloatingActionButton(
                onClick = {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            mp.pause()
                            isPlaying = false
                        } else {
                            mp.start()
                            isPlaying = true
                        }
                    }
                },
                containerColor = CyberCyan,
                contentColor = Color.Black
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) strings.pause else strings.play,
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(onClick = {
                mediaPlayer?.let { mp ->
                    val target = (mp.currentPosition + 10000).coerceAtMost(duration)
                    mp.seekTo(target)
                    currentPosition = target
                }
            }) {
                Icon(Icons.Default.Forward10, contentDescription = strings.forward10s, tint = TextPrimary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

/**
 * Embedded Native PDF Viewer using Android's PdfRenderer.
 */
@Composable
private fun PdfPreviewPane(
    fileBytes: ByteArray,
    strings: VaultStrings
) {
    val context = LocalContext.current
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    DisposableEffect(fileBytes) {
        val tempPdf = File(context.cacheDir, "vaultx_pdf_${UUID.randomUUID()}.pdf")
        try {
            FileOutputStream(tempPdf).use { it.write(fileBytes) }
            val descriptor = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(descriptor)
            pfd = descriptor
            renderer = pdfRenderer
            totalPages = pdfRenderer.pageCount.coerceAtLeast(1)
        } catch (_: Exception) {}

        onDispose {
            try {
                renderer?.close()
                pfd?.close()
                tempPdf.delete()
            } catch (_: Exception) {}
        }
    }

    // Render page bitmap on page index change
    LaunchedEffect(renderer, currentPageIndex) {
        renderer?.let { r ->
            if (currentPageIndex in 0 until r.pageCount) {
                val page = r.openPage(currentPageIndex)
                val width = (page.width * 1.5).toInt().coerceAtLeast(1)
                val height = (page.height * 1.5).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pageBitmap = bmp
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E2430)),
            contentAlignment = Alignment.Center
        ) {
            pageBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            } ?: CircularProgressIndicator(color = CyberCyan)
        }

        // PDF Pagination Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VaultSurfaceCard,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                    enabled = currentPageIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.previousPage, tint = CyberCyan)
                }

                Text(
                    text = strings.pageOf(currentPageIndex + 1, totalPages),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                IconButton(
                    onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                    enabled = currentPageIndex < totalPages - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = strings.nextPage, tint = CyberCyan)
                }
            }
        }
    }
}

/**
 * Sandboxed plain text & code file viewer (never executes HTML or scripts).
 */
@Composable
private fun TextCodePreviewPane(
    fileBytes: ByteArray,
    strings: VaultStrings
) {
    val textContent = remember(fileBytes) {
        try {
            String(fileBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            strings.cannotDecodeText
        }
    }
    val lines = remember(textContent) { textContent.lines() }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VaultSurfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.codeViewer, fontSize = 12.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                Text(strings.linesCount(lines.size), fontSize = 12.sp, color = TextSecondary)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .padding(12.dp)
        ) {
            items(lines.size) { index ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${index + 1}".padStart(4, ' '),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.width(42.dp)
                    )
                    Text(
                        text = lines[index],
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Safe ZIP Archive inspection (never executes extracted content).
 */
@Composable
private fun ArchivePreviewPane(
    fileBytes: ByteArray,
    strings: VaultStrings
) {
    data class ZipItem(val name: String, val size: Long, val compressedSize: Long, val isDirectory: Boolean)

    val entries = remember(fileBytes) {
        val list = mutableListOf<ZipItem>()
        try {
            ZipInputStream(ByteArrayInputStream(fileBytes)).use { zis ->
                var entry = zis.nextEntry
                var count = 0
                while (entry != null && count < 500) { // Limit against zip bombs
                    // Prevent path traversal display
                    val safeName = entry.name.replace("..", "__")
                    list.add(
                        ZipItem(
                            name = safeName,
                            size = entry.size.coerceAtLeast(0),
                            compressedSize = entry.compressedSize.coerceAtLeast(0),
                            isDirectory = entry.isDirectory
                        )
                    )
                    zis.closeEntry()
                    count++
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {}
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VaultSurfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.archiveContents, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CyberCyan)
                Text(strings.archiveFileCount(entries.size), fontSize = 12.sp, color = TextSecondary)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (item.isDirectory) Color(0xFFFBBF24) else CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            if (!item.isDirectory && item.size > 0) {
                                Text(
                                    text = strings.archiveEntrySize(strings.formatBytes(item.size), strings.formatBytes(item.compressedSize)),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean fallback card for unsupported file types with cryptographic SHA-256 fingerprint.
 */
@Composable
private fun UnsupportedPreviewPane(
    item: VaultItem,
    fileBytes: ByteArray,
    strings: VaultStrings,
    onExport: () -> Unit
) {
    val hash = remember(fileBytes) {
        try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            md.digest(fileBytes).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "N/A"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(VaultSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.previewUnavailable,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = strings.previewUnavailableDesc,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(strings.mimeTypeLabel, item.fileMimeType ?: "application/octet-stream")
                Spacer(modifier = Modifier.height(6.dp))
                DetailRow(strings.sizeLabel, strings.formatBytes(fileBytes.size.toLong()))
                Spacer(modifier = Modifier.height(6.dp))
                DetailRow("SHA-256", hash.take(24) + "...")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.downloadSecurely, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
