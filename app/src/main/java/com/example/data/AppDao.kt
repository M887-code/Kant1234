package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // Files
    @Query("SELECT * FROM files WHERE projectId = :projectId ORDER BY path ASC")
    fun getFilesByProject(projectId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE projectId = :projectId AND path = :path LIMIT 1")
    suspend fun getFileByPath(projectId: Long, path: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("DELETE FROM files WHERE projectId = :projectId")
    suspend fun deleteFilesByProjectId(projectId: Long)

    // Terminal History
    @Query("SELECT * FROM terminal_history ORDER BY timestamp DESC LIMIT 100")
    fun getTerminalHistory(): Flow<List<TerminalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalHistory(history: TerminalHistoryEntity)

    @Query("DELETE FROM terminal_history")
    suspend fun clearTerminalHistory()
}
