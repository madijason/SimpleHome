package com.madijason.simplehome

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

// ─── Color Tokens ─────────────────────────────────────────────────────────────
private val LightBg1     = Color(0xFFEAF2EA)
private val LightBg2     = Color(0xFFD8EBE2)
private val LightOnBg    = Color(0xFF1A2B1B)
private val LightSurface = Color(0xFFF4FAF5)
private val LightIconBg  = Color(0xFFEEF5EF)
private val LightAccent  = Color(0xFF3E7D52)
private val LightLabel   = Color(0xFF4A6550)

private val DarkBg1      = Color(0xFF0A0A10)
private val DarkBg2      = Color(0xFF0E0C16)
private val DarkOnBg     = Color(0xFFE4DDD3)
private val DarkSurface  = Color(0xFF14111C)
private val DarkIconBg   = Color(0xFF1C1826)
private val DarkAccent   = Color(0xFF7ABBA0)
private val DarkLabel    = Color(0xFF9B9187)

@Composable
private fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            background   = DarkBg1, surface = DarkSurface,
            onBackground = DarkOnBg, onSurface = DarkOnBg, primary = DarkAccent
        ) else lightColorScheme(
            background   = LightBg1, surface = LightSurface,
            onBackground = LightOnBg, onSurface = LightOnBg, primary = LightAccent
        ),
        content = content
    )
}

// ─── MainActivity ─────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val apps   = remember { getInstalledApps() }
            val pinned = remember { mutableStateListOf<AppInfo>() }
            val dark   = isSystemInDarkTheme()
            AppTheme(dark) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (dark) DarkBg1 else LightBg1,
                                    if (dark) DarkBg2 else LightBg2
                                )
                            )
                        )
                ) {
                    AmbientBlobs(dark)
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                    ) {
                        HomeHeader(dark)
                        Spacer(Modifier.height(28.dp))
                        AppGrid(
                            apps          = apps,
                            pinned        = pinned,
                            dark          = dark,
                            onAppClick    = { launchApp(it.packageName) },
                            onAppLongClick = { togglePin(it, pinned) }
                        )
                    }
                    // Dock slides in/out from the bottom with spring physics
                    AnimatedVisibility(
                        visible  = pinned.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec  = spring(dampingRatio = 0.60f, stiffness = 380f)
                        ) + fadeIn(tween(220)),
                        exit  = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f)
                        ) + fadeOut(tween(160))
                    ) {
                        Dock(
                            pinned            = pinned,
                            dark              = dark,
                            onPinnedClick     = { launchApp(it.packageName) },
                            onPinnedLongClick = { togglePin(it, pinned) }
                        )
                    }
                }
            }
        }
    }

    private fun togglePin(app: AppInfo, pinned: MutableList<AppInfo>) {
        val idx = pinned.indexOfFirst { it.packageName == app.packageName }
        if (idx >= 0) { pinned.removeAt(idx); return }
        if (pinned.size >= 4) return
        pinned.add(app)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm     = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, 0).map {
            AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm))
        }.sortedBy { it.label.lowercase() }
    }

    private fun launchApp(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }
}

// ─── Decorative ambient blobs ─────────────────────────────────────────────────
// Two soft circles drawn behind everything to create an organic depth layer.
@Composable
private fun AmbientBlobs(dark: Boolean) {
    val c1 = if (dark) Color(0xFF191524) else Color(0xFFC0DECA)
    val c2 = if (dark) Color(0xFF130F1E) else Color(0xFFCBE5D8)
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            color  = c1.copy(alpha = 0.55f),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.87f, size.height * 0.12f)
        )
        drawCircle(
            color  = c2.copy(alpha = 0.48f),
            radius = size.width * 0.50f,
            center = Offset(size.width * 0.10f, size.height * 0.78f)
        )
    }
}

// ─── Header with pulsing accent dot ──────────────────────────────────────────
@Composable
private fun HomeHeader(dark: Boolean) {
    val accent = if (dark) DarkAccent else LightAccent
    val title  = if (dark) DarkOnBg   else LightOnBg
    val inf    = rememberInfiniteTransition(label = "pulse")
    val s      by inf.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.35f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dotPulse"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size((9f * s).dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "SimpleHome",
            fontSize      = 34.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = title,
            letterSpacing = (-0.5).sp
        )
    }
}

// ─── App Grid ─────────────────────────────────────────────────────────────────
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    pinned: List<AppInfo>,
    dark: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns               = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement   = Arrangement.spacedBy(20.dp),
        contentPadding        = PaddingValues(bottom = 120.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppIcon(
                app         = app,
                isPinned    = pinned.any { it.packageName == app.packageName },
                dark        = dark,
                onClick     = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
        }
    }
}

// ─── App Icon ─────────────────────────────────────────────────────────────────
// - Spring scale on press for satisfying tactile feedback
// - Colored shadow (ambientColor/spotColor) for depth / glow
// - Pin badge animates in/out with spring scaleIn
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    isPinned: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val src     = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale   by animateFloatAsState(
        targetValue   = if (pressed) 0.87f else 1f,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 700f),
        label = "iconScale"
    )

    val accent   = if (dark) DarkAccent else LightAccent
    val iconBg   = if (dark) DarkIconBg else LightIconBg
    val labelCol = if (dark) DarkLabel  else LightLabel
    val bgColor  = if (dark) DarkBg1    else LightBg1
    val bitmap   = remember(app.packageName) {
        app.icon.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                interactionSource = src,
                indication        = null,
                onClick           = onClick,
                onLongClick       = onLongClick
            )
            .padding(4.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Box {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = app.label,
                modifier           = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation    = if (isPinned) 14.dp else 6.dp,
                        shape        = RoundedCornerShape(22.dp),
                        ambientColor = accent.copy(alpha = if (isPinned) 0.45f else 0.12f),
                        spotColor    = accent.copy(alpha = if (isPinned) 0.55f else 0.18f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(iconBg)
                    .padding(6.dp)
            )
            // Pin badge: springs into existence, fades out cleanly
            androidx.compose.animation.AnimatedVisibility(
                visible  = isPinned,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                enter = scaleIn(spring(dampingRatio = 0.38f, stiffness = 650f)) + fadeIn(tween(100)),
                exit  = scaleOut(tween(120)) + fadeOut(tween(120))
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .shadow(4.dp, CircleShape, spotColor = accent)
                        .clip(CircleShape)
                        .background(accent)
                        .border(1.5.dp, bgColor, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            app.label,
            fontSize  = 11.sp,
            color     = labelCol,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ─── Dock ─────────────────────────────────────────────────────────────────────
// Semi-transparent pill with colored elevation shadow and spring content sizing.
@Composable
fun Dock(
    pinned: List<AppInfo>,
    dark: Boolean,
    onPinnedClick: (AppInfo) -> Unit,
    onPinnedLongClick: (AppInfo) -> Unit
) {
    val accent = if (dark) DarkAccent else LightAccent
    val bg     = if (dark) Color(0xFF1A1626).copy(alpha = 0.94f)
                        else Color(0xFFF3F9F4).copy(alpha = 0.94f)
    val border = if (dark) Color(0xFF2A2436) else Color(0xFFC8D8CE)

    Surface(
        shape    = RoundedCornerShape(28.dp),
        color    = bg,
        border   = BorderStroke(1.dp, border),
        modifier = Modifier
            .shadow(
                elevation    = 24.dp,
                shape        = RoundedCornerShape(28.dp),
                ambientColor = accent.copy(alpha = if (dark) 0.50f else 0.12f),
                spotColor    = accent.copy(alpha = if (dark) 0.60f else 0.20f)
            )
            .animateContentSize(spring(dampingRatio = 0.6f, stiffness = 400f))
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            pinned.take(4).forEach { app ->
                DockIcon(app, dark, { onPinnedClick(app) }, { onPinnedLongClick(app) })
            }
            // Placeholder slots to maintain stable pill width
            repeat(4 - pinned.size.coerceAtMost(4)) {
                Box(Modifier.size(52.dp))
            }
        }
    }
}

// ─── Dock Icon ─────────────────────────────────────────────────────────────────
// Bouncier spring than grid icons for a distinct dock feel.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockIcon(
    app: AppInfo,
    dark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val src     = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale   by animateFloatAsState(
        targetValue   = if (pressed) 0.83f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "dockScale"
    )

    val accent = if (dark) DarkAccent else LightAccent
    val iconBg = if (dark) Color(0xFF221E2C) else Color(0xFFECF4EE)
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)
    }

    Image(
        bitmap             = bitmap.asImageBitmap(),
        contentDescription = app.label,
        modifier           = Modifier
            .size(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation    = if (pressed) 2.dp else 8.dp,
                shape        = RoundedCornerShape(18.dp),
                ambientColor = accent.copy(alpha = 0.20f),
                spotColor    = accent.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(iconBg)
            .combinedClickable(
                interactionSource = src,
                indication        = null,
                onClick           = onClick,
                onLongClick       = onLongClick
            )
            .padding(6.dp)
    )
}
