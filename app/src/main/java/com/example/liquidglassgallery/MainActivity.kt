package com.example.liquidglassgallery

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { LiquidGlassGalleryApp() }
    }
}

private data class GalleryPhoto(
    val id: Long,
    val uri: Uri,
    val dateTakenMillis: Long,
    val displayName: String
)

private data class DemoMemory(
    val title: String,
    val subtitle: String,
    val colors: List<Color>
)

@Composable
private fun LiquidGlassGalleryApp() {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(hasImagePermission(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        hasPermission = hasImagePermission(context)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            photos = loadRecentImages(context)
            isLoading = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF050610),
            surface = Color(0xFF111425),
            primary = Color(0xFFE4F0FF),
            onPrimary = Color(0xFF07101F),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050610))
        ) {
            AuroraBackground()
            LiquidGalleryContent(
                photos = photos,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                hasPermission = hasPermission,
                isLoading = isLoading,
                onRequestPermission = {
                    imageReadPermission()?.let(permissionLauncher::launch)
                },
                onPhotoSelected = { selectedPhoto = it }
            )
            GlassTopBar(
                realPhotoCount = photos.size,
                hasPermission = hasPermission,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            GlassBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            AnimatedVisibility(
                visible = selectedPhoto != null,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(160))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.46f))
                        .clickable { selectedPhoto = null },
                    contentAlignment = Alignment.Center
                ) {
                    selectedPhoto?.let { photo ->
                        PhotoPreviewCard(photo = photo, onClose = { selectedPhoto = null })
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGalleryContent(
    photos: List<GalleryPhoto>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    hasPermission: Boolean,
    isLoading: Boolean,
    onRequestPermission: () -> Unit,
    onPhotoSelected: (GalleryPhoto) -> Unit
) {
    val demo = remember { demoMemories() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 126.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroMemoryCard(
                title = when (selectedTab) {
                    0 -> "今天的光影"
                    1 -> "回忆胶囊"
                    else -> "搜索你的瞬间"
                },
                subtitle = if (hasPermission && photos.isNotEmpty()) {
                    "已读取 ${photos.size} 张最近照片"
                } else {
                    "未授权时显示内置动态玻璃演示卡片"
                },
                onPrimaryAction = onRequestPermission,
                showAction = !hasPermission
            )
        }

        if (!hasPermission) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PermissionGlassCard(onRequestPermission = onRequestPermission)
            }
        }

        if (isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LoadingGlassCard()
            }
        }

        if (photos.isNotEmpty() && selectedTab != 1) {
            items(photos, key = { it.id }) { photo ->
                PhotoTile(photo = photo, onClick = { onPhotoSelected(photo) })
            }
        } else {
            items(demo, key = { it.title }) { memory ->
                DemoPhotoTile(memory = memory, onClick = { onTabSelected((selectedTab + 1) % 3) })
            }
        }
    }
}

@Composable
private fun GlassTopBar(
    realPhotoCount: Int,
    hasPermission: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 34.dp, tint = Color.White.copy(alpha = 0.13f))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "相册",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = if (hasPermission) "Liquid Glass · $realPhotoCount photos" else "Liquid Glass · Demo mode",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        GlassGlyph(text = "⌕")
        Spacer(modifier = Modifier.width(10.dp))
        GlassGlyph(text = "＋")
    }
}

@Composable
private fun GlassBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("图库" to "◒", "回忆" to "◇", "搜索" to "⌕")
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 22.dp, vertical = 16.dp)
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 36.dp, tint = Color.White.copy(alpha = 0.15f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, item ->
            val selected = selectedTab == index
            val selectedAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(220),
                label = "tabAlpha"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.16f * selectedAlpha))
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.second, color = Color.White.copy(alpha = if (selected) 1f else 0.62f), fontSize = 17.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.first,
                    color = Color.White.copy(alpha = if (selected) 1f else 0.62f),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun HeroMemoryCard(
    title: String,
    subtitle: String,
    onPrimaryAction: () -> Unit,
    showAction: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .liquidGlass(cornerRadius = 38.dp, tint = Color.White.copy(alpha = 0.12f))
            .padding(22.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9FE7FF).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.05f),
                    radius = size.minDimension * 1.05f
                ),
                radius = size.minDimension * 0.9f,
                center = Offset(size.width * 0.78f, size.height * 0.05f),
                blendMode = BlendMode.Screen
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFB5E7).copy(alpha = 0.52f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.95f),
                    radius = size.minDimension
                ),
                radius = size.minDimension * 0.86f,
                center = Offset(size.width * 0.1f, size.height * 0.95f),
                blendMode = BlendMode.Screen
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(text = "精选", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
            Text(
                text = title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showAction) {
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.88f),
                    contentColor = Color(0xFF07101F)
                ),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text("读取照片")
            }
        }
    }
}

@Composable
private fun PermissionGlassCard(onRequestPermission: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 30.dp, tint = Color.White.copy(alpha = 0.11f))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .liquidGlass(cornerRadius = 22.dp, tint = Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("◈", color = Color.White, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("允许访问照片后会显示系统相册", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("未授权也能直接编译和运行，界面会显示演示照片。", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
        }
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.88f),
                contentColor = Color(0xFF07101F)
            )
        ) {
            Text("授权")
        }
    }
}

@Composable
private fun LoadingGlassCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 28.dp, tint = Color.White.copy(alpha = 0.1f))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Color.White,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text("正在折射最近的照片……", color = Color.White.copy(alpha = 0.82f))
    }
}

@Composable
private fun PhotoTile(photo: GalleryPhoto, onClick: () -> Unit) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = photo.uri) {
        value = loadImageBitmap(context = context, uri = photo.uri, maxSize = 720)
    }
    Box(
        modifier = Modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .liquidGlass(cornerRadius = 30.dp, tint = Color.White.copy(alpha = 0.08f))
    ) {
        if (image != null) {
            Image(
                bitmap = image!!,
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            TilePlaceholder(photo.id)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.56f))
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = formatDate(photo.dateTakenMillis),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DemoPhotoTile(memory: DemoMemory, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .liquidGlass(cornerRadius = 30.dp, tint = Color.White.copy(alpha = 0.08f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(memory.colors, start = Offset.Zero, end = Offset(size.width, size.height)))
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)),
                radius = size.minDimension * 0.52f,
                center = Offset(size.width * 0.82f, size.height * 0.12f),
                blendMode = BlendMode.Screen
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
                radius = size.minDimension * 0.82f,
                center = Offset(size.width * 0.1f, size.height * 0.85f),
                blendMode = BlendMode.Screen
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f))))
                .padding(14.dp)
        ) {
            Text(memory.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
            Text(memory.subtitle, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TilePlaceholder(seed: Long) {
    val palettes = remember { demoMemories() }
    val colors = palettes[(seed % palettes.size).toInt()].colors
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height)))
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)),
            radius = size.minDimension * 0.72f,
            center = Offset(size.width * 0.75f, size.height * 0.25f),
            blendMode = BlendMode.Screen
        )
    }
}

@Composable
private fun PhotoPreviewCard(photo: GalleryPhoto, onClose: () -> Unit) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = photo.uri) {
        value = loadImageBitmap(context = context, uri = photo.uri, maxSize = 1280)
    }
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 38.dp, tint = Color.White.copy(alpha = 0.14f))
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(RoundedCornerShape(30.dp))
        ) {
            if (image != null) {
                Image(
                    bitmap = image!!,
                    contentDescription = photo.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                TilePlaceholder(seed = photo.id)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(photo.displayName.ifBlank { "照片" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDate(photo.dateTakenMillis), color = Color.White.copy(alpha = 0.64f), fontSize = 12.sp)
            }
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.86f),
                    contentColor = Color(0xFF07101F)
                )
            ) {
                Text("关闭")
            }
        }
    }
}

@Composable
private fun GlassGlyph(text: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .liquidGlass(cornerRadius = 20.dp, tint = Color.White.copy(alpha = 0.17f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AuroraBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color(0xFF050610), Color(0xFF081426), Color(0xFF140923)),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF5CE1E6).copy(alpha = 0.5f), Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.2f),
                radius = size.minDimension * 0.85f
            ),
            radius = size.minDimension * 0.85f,
            center = Offset(size.width * 0.12f, size.height * 0.2f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF8FD8).copy(alpha = 0.42f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.16f),
                radius = size.minDimension * 0.8f
            ),
            radius = size.minDimension * 0.8f,
            center = Offset(size.width * 0.88f, size.height * 0.16f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF9B7DFF).copy(alpha = 0.46f), Color.Transparent),
                center = Offset(size.width * 0.62f, size.height * 0.82f),
                radius = size.minDimension
            ),
            radius = size.minDimension,
            center = Offset(size.width * 0.62f, size.height * 0.82f)
        )
    }
}

private fun Modifier.liquidGlass(
    cornerRadius: Dp = 32.dp,
    tint: Color = Color.White.copy(alpha = 0.12f),
    borderAlpha: Float = 0.58f
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val radius = with(LocalDensity.current) { cornerRadius.toPx() }
    this
        .shadow(elevation = 20.dp, shape = shape, clip = false)
        .clip(shape)
        .background(tint)
        .then(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f),
                        Color(0xFF9FE7FF).copy(alpha = 0.08f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 1200f)
                )
            )
        )
        .then(
            Modifier.composed {
                Modifier.drawGlassOverlay(radius = radius, borderAlpha = borderAlpha)
            }
        )
}

private fun Modifier.drawGlassOverlay(radius: Float, borderAlpha: Float): Modifier = this.drawWithCache {
    val border = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = borderAlpha),
            Color.White.copy(alpha = 0.12f),
            Color(0xFFBFEAFF).copy(alpha = borderAlpha * 0.72f)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    val sheen = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.26f),
            Color.Transparent,
            Color.White.copy(alpha = 0.08f)
        ),
        start = Offset(size.width * 0.05f, 0f),
        end = Offset(size.width, size.height * 0.72f)
    )
    onDrawWithContent {
        drawContent()
        drawRoundRect(
            brush = sheen,
            cornerRadius = CornerRadius(radius, radius),
            blendMode = BlendMode.Screen
        )
        drawRoundRect(
            brush = border,
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = 1.25.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.13f),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.18f, size.height * 0.08f),
            blendMode = BlendMode.Screen
        )
    }
}

private fun imageReadPermission(): String? {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun hasImagePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) {
        val allImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val selectedImages = if (Build.VERSION.SDK_INT >= 34) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        allImages || selectedImages
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private suspend fun loadRecentImages(context: Context, limit: Int = 240): List<GalleryPhoto> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<GalleryPhoto>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        while (cursor.moveToNext() && photos.size < limit) {
            val id = cursor.getLong(idColumn)
            val dateTaken = cursor.getLong(takenColumn).takeIf { it > 0L }
            val dateAdded = cursor.getLong(addedColumn).takeIf { it > 0L }?.let { it * 1000L }
            photos += GalleryPhoto(
                id = id,
                uri = ContentUris.withAppendedId(collection, id),
                dateTakenMillis = dateTaken ?: dateAdded ?: System.currentTimeMillis(),
                displayName = cursor.getString(nameColumn).orEmpty()
            )
        }
    }
    photos
}

private suspend fun loadImageBitmap(context: Context, uri: Uri, maxSize: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                val width = info.size.width
                val height = info.size.height
                val scale = (maxSize.toFloat() / max(width, height).toFloat()).coerceAtMost(1f)
                decoder.setTargetSize((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
            }.asImageBitmap()
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { bitmap ->
                val maxDimension = max(bitmap.width, bitmap.height)
                if (maxDimension <= maxSize) {
                    bitmap.asImageBitmap()
                } else {
                    val scale = maxSize.toFloat() / maxDimension.toFloat()
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).roundToInt().coerceAtLeast(1),
                        (bitmap.height * scale).roundToInt().coerceAtLeast(1),
                        true
                    ).asImageBitmap()
                }
            }
        }
    }.getOrNull()
}

private fun formatDate(timeMillis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timeMillis))
}

private fun demoMemories(): List<DemoMemory> = listOf(
    DemoMemory("晨雾", "透明栏与柔光边缘", listOf(Color(0xFF6EE7FF), Color(0xFF9B7DFF), Color(0xFFFF8FD8))),
    DemoMemory("蓝调", "玻璃高光卡片", listOf(Color(0xFF133C75), Color(0xFF45C4FF), Color(0xFFBDEBFF))),
    DemoMemory("晚霞", "折射式底部导航", listOf(Color(0xFFFF8A65), Color(0xFFFFD180), Color(0xFF7E57C2))),
    DemoMemory("霓虹", "液态浮层预览", listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF4081))),
    DemoMemory("月光", "柔和透明岛", listOf(Color(0xFF0B1026), Color(0xFF274C77), Color(0xFFE0FBFC))),
    DemoMemory("森林", "半透明内容聚焦", listOf(Color(0xFF064E3B), Color(0xFF34D399), Color(0xFFA7F3D0)))
)
