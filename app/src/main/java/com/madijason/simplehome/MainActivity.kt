package com.madijason.simplehome

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

private val LightBackground = Color(0xFFF0F4F2) // organic sage
private val LightOnBackground = Color(0xFF2C3E2D)
private val LightSurface = Color.White
private val LightOnSurface = Color(0xFF2C3E2D)

private val DarkBackground = Color(0xFF0B0B0B) // near-black
private val DarkSurface = Color(0xFF1A1410) // deep brown
private val DarkOnBackground = Color(0xFFF2E9DF) // warm off-white
private val DarkOnSurface = Color(0xFFF2E9DF)
private val DarkMutedText = Color(0xFFCABFB3)

@Composable
private fun SimpleHomeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = DarkOnBackground,
            onSurface = DarkOnSurface
        )
    } else {
        lightColorScheme(
            background = LightBackground,
            surface = LightSurface,
            onBackground = LightOnBackground,
            onSurface = LightOnSurface
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val apps = remember { getInstalledApps() }
            val pinned = remember { mutableStateListOf<AppInfo>() }
            val darkTheme = isSystemInDarkTheme()

            SimpleHomeTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                        ) {
                            Text(
                                text = "SimpleHome",
                                fontSize = 36.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            AppGrid(
                                apps = apps,
                                pinned = pinned,
                                onAppClick = { app -> launchApp(app.packageName) },
                                onAppLongClick = { app ->
                                    togglePin(app, pinned)
                                }
                            )
                        }

                        PinnedBar(
                            pinned = pinned,
                            darkTheme = darkTheme,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            onPinnedClick = { app -> launchApp(app.packageName) },
                            onPinnedLongClick = { app -> togglePin(app, pinned) }
                        )
                    }
                }
            }
        }
    }

    private fun togglePin(app: AppInfo, pinned: MutableList<AppInfo>) {
        val existingIndex = pinned.indexOfFirst { it.packageName == app.packageName }
        if (existingIndex >= 0) {
            pinned.removeAt(existingIndex)
            return
        }

        if (pinned.size >= 4) return
        pinned.add(app)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm: PackageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        return resolveInfos.map {
            AppInfo(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(pm)
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let { startActivity(it) }
    }
}

@Composable
fun AppGrid(
    apps: List<AppInfo>,
    pinned: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 110.dp) // keep content above floating bar
    ) {
        items(apps) { app ->
            val isPinned = pinned.any { it.packageName == app.packageName }
            AppIcon(
                app = app,
                isPinned = isPinned,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
        }
    }
}

@Composable
fun AppIcon(
    app: AppInfo,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val iconSurface = if (darkTheme) Color(0xFF221A14) else Color.White
    val labelColor = if (darkTheme) DarkMutedText else Color(0xFF4A554A)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
    ) {
        val bitmap = app.icon.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)

        Box {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(iconSurface)
                    .padding(6.dp)
            )

            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = app.label,
            fontSize = 12.sp,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PinnedBar(
    pinned: List<AppInfo>,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onPinnedClick: (AppInfo) -> Unit,
    onPinnedLongClick: (AppInfo) -> Unit
) {
    if (pinned.isEmpty()) return

    val barColor = if (darkTheme) Color(0xFF14110E) else Color.White
    val barStroke = if (darkTheme) Color(0xFF2A231D) else Color(0xFFE6EAE7)

    Surface(
        modifier = modifier,
        color = barColor,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 2.dp,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, barStroke)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pinned.take(4).forEach { app ->
                PinnedIcon(
                    app = app,
                    onClick = { onPinnedClick(app) },
                    onLongClick = { onPinnedLongClick(app) }
                )
            }

            // placeholders for consistent spacing up to 4
            repeat(4 - pinned.size.coerceAtMost(4)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun PinnedIcon(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val iconSurface = if (darkTheme) Color(0xFF221A14) else Color.White

    val bitmap = app.icon.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = app.label,
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(iconSurface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp)
    )
}
