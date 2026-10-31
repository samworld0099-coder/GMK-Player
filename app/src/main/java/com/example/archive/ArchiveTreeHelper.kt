package com.example.archive

import com.example.data.model.ArchiveEntryItem

data class ArchiveDirectoryNode(
    val currentPath: String, // e.g. "" or "photos/vacation"
    val subfolders: List<ArchiveEntryItem>,
    val files: List<ArchiveEntryItem>
)

object ArchiveTreeHelper {

    /**
     * Given all archive entries, filters and lists the immediate children for the given virtual folder path.
     * @param rootEntries Flat list of entries from the archive
     * @param directoryPath Current folder path, empty string "" for archive root
     */
    fun getEntriesInDirectory(
        rootEntries: List<ArchiveEntryItem>,
        directoryPath: String
    ): ArchiveDirectoryNode {
        val normalizedDir = directoryPath.trim('/').replace('\\', '/')
        val prefix = if (normalizedDir.isEmpty()) "" else "$normalizedDir/"

        val immediateFolders = mutableMapOf<String, ArchiveEntryItem>()
        val immediateFiles = mutableListOf<ArchiveEntryItem>()

        for (entry in rootEntries) {
            val entryPath = entry.path.replace('\\', '/')
            if (prefix.isNotEmpty() && !entryPath.startsWith(prefix)) {
                continue
            }
            if (entryPath == normalizedDir) {
                continue
            }

            val relative = if (prefix.isEmpty()) entryPath else entryPath.removePrefix(prefix)
            if (relative.isEmpty()) continue

            val slashIdx = relative.indexOf('/')
            if (slashIdx >= 0) {
                // It is a subfolder or item inside a subfolder
                val folderName = relative.substring(0, slashIdx)
                val fullFolderPath = if (prefix.isEmpty()) folderName else "$prefix$folderName"
                if (!immediateFolders.containsKey(folderName)) {
                    immediateFolders[folderName] = ArchiveEntryItem(
                        path = fullFolderPath,
                        name = folderName,
                        isDirectory = true,
                        size = 0L,
                        compressedSize = 0L,
                        lastModified = entry.lastModified
                    )
                }
            } else {
                // Immediate item
                if (entry.isDirectory) {
                    if (!immediateFolders.containsKey(relative)) {
                        immediateFolders[relative] = entry.copy(name = relative)
                    }
                } else {
                    immediateFiles.add(entry.copy(name = relative))
                }
            }
        }

        val sortedFolders = immediateFolders.values.sortedBy { it.name.lowercase() }
        val sortedFiles = immediateFiles.sortedBy { it.name.lowercase() }

        return ArchiveDirectoryNode(
            currentPath = normalizedDir,
            subfolders = sortedFolders,
            files = sortedFiles
        )
    }

    /**
     * Splits a path into breadcrumb items for navigation.
     * e.g. "a/b/c" -> [("Root", ""), ("a", "a"), ("b", "a/b"), ("c", "a/b/c")]
     */
    fun getBreadcrumbs(currentPath: String): List<Pair<String, String>> {
        val crumbs = mutableListOf("Archive Root" to "")
        if (currentPath.isBlank()) return crumbs

        val parts = currentPath.split('/').filter { it.isNotBlank() }
        var acc = ""
        for (part in parts) {
            acc = if (acc.isEmpty()) part else "$acc/$part"
            crumbs.add(part to acc)
        }
        return crumbs
    }
}
