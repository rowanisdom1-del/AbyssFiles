@file:OptIn(ExperimentalFoundationApi::class)

package com.rowan.abyssfiles

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface Dlg {
    object NewFolder : Dlg
    data class Rename(val file: File) : Dlg
    object Delete : Dlg
}

// ---------------------------------------------------------------- root

@Composable
fun FilesApp(vm: FilesViewModel, granted: Boolean, onGrant: () -> Unit, onOpen: (File) -> Unit) {
    Box(Modifier.fillMaxSize().background(Abyss.backdrop)) {
        // soft violet glow in the corner, like the Abyss theme's backdrop blur
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Abyss.accent2.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(1700f, 0f),
                    radius = 1300f,
                )
            )
        )
        if (granted) Browser(vm, onOpen) else PermissionGate(onGrant)
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FolderGlyph(72.dp, Abyss.accent)
        Spacer(Modifier.height(20.dp))
        T("Abyss Files", 32, weight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        T("Allow access to storage so you can browse your files.", 16, Abyss.dim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        FocusBox(onClick = onGrant, modifier = Modifier.focusRequester(focus), shape = RoundedCornerShape(50), base = Abyss.accent.copy(alpha = 0.25f)) {
            T("Allow access", 16, weight = FontWeight.SemiBold, modifier = Modifier.padding(28.dp, 14.dp))
        }
    }
}

// ---------------------------------------------------------------- browser

@Composable
private fun Browser(vm: FilesViewModel, onOpen: (File) -> Unit) {
    var dlg by remember { mutableStateOf<Dlg?>(null) }

    BackHandler(enabled = vm.selected.isNotEmpty() || !vm.atRoot) {
        if (vm.selected.isNotEmpty()) vm.selected.clear() else vm.up()
    }
    LaunchedEffect(vm.toast) {
        if (vm.toast != null) {
            delay(2500)
            vm.toast = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Sidebar(vm, Modifier.width(250.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight().padding(28.dp)) {
                Header(vm)
                Spacer(Modifier.height(18.dp))
                if (vm.selected.isEmpty()) {
                    Toolbar(vm, onNew = { dlg = Dlg.NewFolder })
                } else {
                    SelectionBar(vm, onRename = { dlg = Dlg.Rename(it) }, onDelete = { dlg = Dlg.Delete })
                }
                Spacer(Modifier.height(16.dp))
                FileList(vm, onOpen, Modifier.weight(1f).fillMaxWidth())
            }
        }
        val message = vm.toast ?: if (vm.busy) "Working…" else null
        if (message != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(50)).background(Abyss.surfaceHi)
                    .border(1.dp, Abyss.accent.copy(alpha = 0.4f), RoundedCornerShape(50))
            ) {
                T(message, 15, modifier = Modifier.padding(24.dp, 12.dp))
            }
        }
    }

    when (val d = dlg) {
        Dlg.NewFolder -> NameDialog("New folder", "", "Create", { dlg = null }) {
            vm.newFolder(it); dlg = null
        }
        is Dlg.Rename -> NameDialog("Rename", d.file.name, "Rename", { dlg = null }) {
            vm.rename(d.file, it); dlg = null
        }
        Dlg.Delete -> AlertDialog(
            onDismissRequest = { dlg = null },
            containerColor = Abyss.surface,
            title = { Text("Delete ${vm.selected.size} item(s)?", color = Abyss.text) },
            text = { Text("This can't be undone.", color = Abyss.dim) },
            confirmButton = { TextButton(onClick = { vm.delete(); dlg = null }) { Text("Delete", color = Abyss.danger) } },
            dismissButton = { TextButton(onClick = { dlg = null }) { Text("Cancel") } },
        )
        null -> {}
    }
}

@Composable
private fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Abyss.surface,
        title = { Text(title, color = Abyss.text) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onOk(text.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------- sidebar + header

@Composable
private fun Sidebar(vm: FilesViewModel, modifier: Modifier) {
    Column(modifier.background(Abyss.bg2.copy(alpha = 0.7f)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FolderGlyph(34.dp, Abyss.accent)
            Spacer(Modifier.width(12.dp))
            Column {
                T("Abyss", 22, weight = FontWeight.Bold)
                T("Files", 13, Abyss.dim)
            }
        }
        Spacer(Modifier.height(26.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vm.places, key = { it.dir.absolutePath }) { p ->
                val active = vm.current == p.dir
                FocusBox(
                    onClick = { vm.go(p.dir) },
                    modifier = Modifier.fillMaxWidth(),
                    base = if (active) Abyss.accent.copy(alpha = 0.18f) else Color.Transparent,
                ) {
                    T(p.label, 15, if (active) Abyss.accent else Abyss.text, modifier = Modifier.padding(16.dp, 13.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(vm: FilesViewModel) {
    Column {
        T(vm.current.name.ifEmpty { "/" }, 30, weight = FontWeight.Bold)
        T(vm.current.absolutePath, 13, Abyss.dim)
        Spacer(Modifier.height(10.dp))
        val total = vm.current.totalSpace
        if (total > 0) {
            val used = (total - vm.current.usableSpace).coerceAtLeast(0)
            val frac = (used.toFloat() / total).coerceIn(0f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(320.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Abyss.surfaceHi)) {
                    Box(
                        Modifier.fillMaxWidth(frac).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Abyss.accent, Abyss.accent2)))
                    )
                }
                Spacer(Modifier.width(12.dp))
                T("${fmtSize(used)} of ${fmtSize(total)} used", 12, Abyss.dim)
            }
        }
    }
}

// ---------------------------------------------------------------- toolbars

@Composable
private fun Toolbar(vm: FilesViewModel, onNew: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pill("New folder", onNew)
        if (vm.clip.isNotEmpty()) Pill("Paste (${vm.clip.size})", { vm.paste() }, accent = true)
        Pill("Sort: ${vm.sort.label}", { vm.cycleSort() })
        Pill(if (vm.showHidden) "Hidden: on" else "Hidden: off", { vm.toggleHidden() })
        Pill("Refresh", { vm.refresh() })
    }
}

@Composable
private fun SelectionBar(vm: FilesViewModel, onRename: (File) -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        T("${vm.selected.size} selected", 15, Abyss.accent, FontWeight.SemiBold, modifier = Modifier.padding(end = 6.dp))
        Pill("Copy", { vm.copy() })
        Pill("Cut", { vm.cut() })
        if (vm.selected.size == 1) Pill("Rename", { onRename(vm.selected[0]) })
        Pill("Delete", onDelete)
        Pill("Select all", { vm.selectAll() })
        Pill("Cancel", { vm.selected.clear() })
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit, accent: Boolean = false) {
    FocusBox(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        base = if (accent) Abyss.accent.copy(alpha = 0.25f) else Abyss.surface,
    ) {
        T(text, 14, modifier = Modifier.padding(18.dp, 10.dp))
    }
}

// ---------------------------------------------------------------- list

@Composable
private fun FileList(vm: FilesViewModel, onOpen: (File) -> Unit, modifier: Modifier) {
    if (vm.items.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { T("This folder is empty", 16, Abyss.dim) }
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 60.dp)) {
        items(vm.items, key = { it.absolutePath }) { f ->
            val sel = f in vm.selected
            val sub = remember(f.absolutePath, f.lastModified()) {
                if (f.isDirectory) "${f.list()?.size ?: 0} items" else fmtSize(f.length())
            }
            FocusBox(
                onClick = {
                    when {
                        vm.selected.isNotEmpty() -> vm.toggle(f)
                        f.isDirectory -> vm.go(f)
                        else -> onOpen(f)
                    }
                },
                onLongClick = { vm.toggle(f) },
                modifier = Modifier.fillMaxWidth(),
                base = if (sel) Abyss.accent.copy(alpha = 0.2f) else Abyss.surface.copy(alpha = 0.6f),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FileGlyph(f)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        T(f.name, 16)
                        T(sub, 12, Abyss.dim)
                    }
                    T(fmtDate(f.lastModified()), 13, Abyss.dim)
                    if (sel) T("  ✓", 18, Abyss.accent, FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FileGlyph(f: File) {
    if (f.isDirectory) {
        FolderGlyph(40.dp, Abyss.accent)
    } else {
        val ext = f.extension.lowercase()
        val c = extColor(ext)
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            T(ext.take(4).uppercase().ifEmpty { "FILE" }, 10, c, FontWeight.Bold)
        }
    }
}

@Composable
private fun FolderGlyph(dp: Dp, color: Color) {
    Canvas(Modifier.size(dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(color.copy(alpha = 0.7f), Offset(0f, h * 0.08f), Size(w * 0.42f, h * 0.24f), CornerRadius(8f))
        drawRoundRect(color, Offset(0f, h * 0.22f), Size(w, h * 0.70f), CornerRadius(10f))
    }
}

private fun extColor(ext: String): Color = when (ext) {
    "mp4", "mkv", "avi", "mov", "webm", "ts", "m4v" -> Abyss.accent2
    "mp3", "flac", "wav", "ogg", "m4a", "opus" -> Abyss.ok
    "jpg", "jpeg", "png", "gif", "webp", "bmp" -> Abyss.pink
    "zip", "rar", "7z", "tar", "gz" -> Color(0xFFFFC857)
    "apk" -> Color(0xFF8DE969)
    else -> Abyss.dim
}

// ---------------------------------------------------------------- building blocks

/** Focusable (D-pad / controller friendly) surface with a glow ring + slight zoom when focused. */
@Composable
private fun FocusBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    base: Color = Abyss.surface,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val zoom by animateFloatAsState(if (focused) 1.02f else 1f, label = "zoom")
    Box(
        modifier
            .scale(zoom)
            .clip(shape)
            .background(if (focused) Abyss.surfaceHi else base)
            .border(2.dp, if (focused) Abyss.accent else Color.Transparent, shape)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        content = content,
    )
}

@Composable
private fun T(
    text: String,
    size: Int = 14,
    color: Color = Abyss.text,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

fun fmtSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    while (v >= 1024 && i < units.size - 1) {
        v /= 1024
        i++
    }
    return String.format(Locale.US, "%.1f %s", v, units[i])
}

private fun fmtDate(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ms))
