package com.rowan.abyssfiles

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SortBy(val label: String) { NAME("Name"), SIZE("Size"), DATE("Date") }

data class Place(val label: String, val dir: File, val isRoot: Boolean = false)

class FilesViewModel : ViewModel() {

    var places by mutableStateOf<List<Place>>(emptyList())
        private set
    var current by mutableStateOf(File("/"))
        private set
    var items by mutableStateOf<List<File>>(emptyList())
        private set
    var sort by mutableStateOf(SortBy.NAME)
        private set
    var showHidden by mutableStateOf(false)
        private set
    var clip by mutableStateOf<List<File>>(emptyList())
        private set
    var busy by mutableStateOf(false)
        private set
    var toast by mutableStateOf<String?>(null)

    val selected = mutableStateListOf<File>()

    private var clipCut = false
    private var started = false

    val atRoot: Boolean
        get() = places.any { it.isRoot && it.dir == current } || current.parentFile == null

    /** Called once storage permission has been granted. */
    fun start() {
        if (started) return
        started = true
        val internal = Environment.getExternalStorageDirectory()
        val list = mutableListOf(Place("Internal storage", internal, true))
        listOf(
            "Downloads" to Environment.DIRECTORY_DOWNLOADS,
            "Movies" to Environment.DIRECTORY_MOVIES,
            "Music" to Environment.DIRECTORY_MUSIC,
            "Pictures" to Environment.DIRECTORY_PICTURES,
        ).forEach { (label, dir) ->
            list += Place(label, Environment.getExternalStoragePublicDirectory(dir))
        }
        File("/storage").listFiles()
            ?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" && it.canRead() }
            ?.forEach { list += Place("SD / USB (${it.name})", it, true) }
        places = list
        go(internal)
    }

    fun go(dir: File) {
        current = dir
        selected.clear()
        refresh()
    }

    fun up() {
        if (!atRoot) current.parentFile?.let { go(it) }
    }

    fun refresh() {
        val files = current.listFiles()?.toList() ?: emptyList()
        items = files.filter { showHidden || !it.name.startsWith(".") }.sortedWith(comparator())
        selected.removeAll { it !in items }
    }

    private fun comparator(): Comparator<File> {
        val base: Comparator<File> = when (sort) {
            SortBy.NAME -> compareBy { it.name.lowercase() }
            SortBy.SIZE -> compareByDescending { if (it.isDirectory) 0L else it.length() }
            SortBy.DATE -> compareByDescending { it.lastModified() }
        }
        return compareBy<File> { !it.isDirectory }.then(base)
    }

    fun cycleSort() {
        sort = SortBy.values()[(sort.ordinal + 1) % SortBy.values().size]
        refresh()
    }

    fun toggleHidden() {
        showHidden = !showHidden
        refresh()
    }

    // ---- selection ----

    fun toggle(f: File) {
        if (f in selected) selected.remove(f) else selected.add(f)
    }

    fun selectAll() {
        selected.clear()
        selected.addAll(items)
    }

    // ---- clipboard ----

    fun copy() {
        clip = selected.toList()
        clipCut = false
        selected.clear()
        toast = "${clip.size} copied"
    }

    fun cut() {
        clip = selected.toList()
        clipCut = true
        selected.clear()
        toast = "${clip.size} ready to move"
    }

    fun paste() {
        val sources = clip
        val moving = clipCut
        val target = current
        if (sources.isEmpty()) return
        viewModelScope.launch {
            busy = true
            val done = withContext(Dispatchers.IO) {
                var n = 0
                for (f in sources) {
                    try {
                        // Never paste a folder into itself.
                        if (f.isDirectory && (target == f || target.absolutePath.startsWith(f.absolutePath + "/"))) continue
                        val dest = uniqueTarget(target, f.name)
                        if (moving) {
                            if (!f.renameTo(dest)) {
                                f.copyRecursively(dest)
                                f.deleteRecursively()
                            }
                        } else {
                            f.copyRecursively(dest)
                        }
                        n++
                    } catch (_: Exception) {
                    }
                }
                n
            }
            if (moving) clip = emptyList()
            toast = "Pasted $done of ${sources.size}"
            busy = false
            refresh()
        }
    }

    // ---- file operations ----

    fun delete() {
        val targets = selected.toList()
        selected.clear()
        viewModelScope.launch {
            busy = true
            val n = withContext(Dispatchers.IO) {
                targets.count { runCatching { it.deleteRecursively() }.getOrDefault(false) }
            }
            toast = "Deleted $n of ${targets.size}"
            busy = false
            refresh()
        }
    }

    fun rename(f: File, name: String) {
        val t = File(f.parentFile, name)
        toast = when {
            t.exists() -> "That name already exists"
            f.renameTo(t) -> "Renamed"
            else -> "Rename failed"
        }
        selected.clear()
        refresh()
    }

    fun newFolder(name: String) {
        val d = File(current, name)
        toast = when {
            d.exists() -> "Already exists"
            d.mkdirs() -> "Folder created"
            else -> "Could not create folder"
        }
        refresh()
    }

    private fun uniqueTarget(dir: File, name: String): File {
        var t = File(dir, name)
        if (!t.exists()) return t
        val base = t.nameWithoutExtension
        val ext = t.extension.let { if (it.isEmpty()) "" else ".$it" }
        var i = 1
        do {
            t = File(dir, "$base ($i)$ext")
            i++
        } while (t.exists())
        return t
    }
}
