package com.rowan.abyssfiles.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Featured(val label: String, val pkg: String?, val tint: Color)
private class AppEntry(val label: String, val pkg: String, val icon: ImageBitmap)

private val featured = listOf(
    Featured("Jellyfin", "org.jellyfin.androidtv", Color(0xFFAA5CC3)),
    Featured("Kodi", "org.xbmc.kodi", Color(0xFF17B2E7)),
    Featured("Plex", "com.plexapp.android", Color(0xFFE5A00D)),
    Featured("Files", "com.rowan.abyssfiles", Abyss.accent),
    Featured("Settings", null, Color(0xFF8A94B2)),
)

@Composable
fun Home(refresh: Int) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10_000)
        }
    }
    val apps = remember(refresh) { loadApps(ctx) }

    Box(Modifier.fillMaxSize().background(Abyss.backdrop)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Abyss.accent2.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(1700f, 0f),
                    radius = 1400f,
                )
            )
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 40.dp)) {
            Column {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                    color = Abyss.text, fontSize = 64.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now),
                    color = Abyss.dim, fontSize = 18.sp,
                )
            }
            Spacer(Modifier.height(44.dp))
            SectionTitle("Watch & browse")
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(22.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)) {
                items(featured) { f ->
                    Tile(f.label, f.tint) {
                        if (f.pkg == null) {
                            ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } else {
                            launchPackage(ctx, f.pkg)
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            SectionTitle("All apps")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)) {
                items(apps, key = { it.pkg }) { a -> AppItem(a) { launchPackage(ctx, a.pkg) } }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Abyss.dim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Tile(label: String, tint: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val zoom by animateFloatAsState(if (focused) 1.08f else 1f, label = "zoom")
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.width(230.dp).height(130.dp).scale(zoom).clip(shape)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.55f), Abyss.surface)))
            .border(if (focused) 3.dp else 1.dp, if (focused) Abyss.accent else Color.White.copy(alpha = 0.08f), shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(label, color = Abyss.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun AppItem(app: AppEntry, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val zoom by animateFloatAsState(if (focused) 1.1f else 1f, label = "zoom")
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(104.dp).scale(zoom).clip(shape)
            .background(if (focused) Abyss.surfaceHi else Color.Transparent)
            .border(2.dp, if (focused) Abyss.accent else Color.Transparent, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(app.icon, contentDescription = app.label, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            app.label, color = Abyss.text, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun launchPackage(ctx: Context, pkg: String) {
    val pm = ctx.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
    if (intent != null) {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        Toast.makeText(ctx, "Not installed yet", Toast.LENGTH_SHORT).show()
    }
}

private fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val seen = HashSet<String>()
    val out = mutableListOf<AppEntry>()
    for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
        val query = Intent(Intent.ACTION_MAIN).addCategory(category)
        for (info in pm.queryIntentActivities(query, 0)) {
            val pkg = info.activityInfo.packageName
            if (pkg == ctx.packageName || !seen.add(pkg)) continue
            out += AppEntry(
                info.loadLabel(pm).toString(),
                pkg,
                info.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
            )
        }
    }
    return out.sortedBy { it.label.lowercase() }
}
