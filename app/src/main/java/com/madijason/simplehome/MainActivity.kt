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
import androidx.compose.foundation.clickable
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val apps = getInstalledApps()
        
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFF0F4F2), // Organic light sage background
                    surface = Color.White,
                    onBackground = Color(0xFF2C3E2D) // Dark green-gray text
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                    ) {
                        Text(
                            text = "SimpleHome",
                            fontSize = 36.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        AppGrid(apps = apps) { app ->
                            launchApp(app.packageName)
                        }
                    }
                }
            }
        }
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
fun AppGrid(apps: List<AppInfo>, onAppClick: (AppInfo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(apps) { app ->
            AppIcon(app = app, onClick = { onAppClick(app) })
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        val bitmap = app.icon.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp)) // Organic rounded look
                .background(Color.White)
                .padding(6.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = app.label,
            fontSize = 12.sp,
            color = Color(0xFF4A554A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}