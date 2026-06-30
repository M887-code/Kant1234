package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val language: String, // "Web", "Kotlin", "Python", "NodeJS"
    val createdAt: Long = System.currentTimeMillis(),
    val packageName: String = "com.mycompany.myapp",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val appIconColor: Int = 0xFF6200EE.toInt() // Purple
)

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val path: String, // relative path e.g. "index.html", "src/MainActivity.kt"
    val content: String,
    val language: String // "html", "kotlin", "python", "js", "css", "json", "sh"
)

@Entity(tableName = "terminal_history")
data class TerminalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)
