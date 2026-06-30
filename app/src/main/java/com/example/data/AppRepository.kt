package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allProjects: Flow<List<ProjectEntity>> = appDao.getAllProjects()
    val terminalHistory: Flow<List<TerminalHistoryEntity>> = appDao.getTerminalHistory()

    suspend fun getProjectById(id: Long) = appDao.getProjectById(id)

    suspend fun insertProject(project: ProjectEntity): Long = appDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = appDao.updateProject(project)

    suspend fun deleteProject(id: Long) {
        appDao.deleteFilesByProjectId(id)
        appDao.deleteProjectById(id)
    }

    fun getFilesByProject(projectId: Long): Flow<List<FileEntity>> = appDao.getFilesByProject(projectId)

    suspend fun getFileByPath(projectId: Long, path: String) = appDao.getFileByPath(projectId, path)

    suspend fun insertFile(file: FileEntity): Long = appDao.insertFile(file)

    suspend fun updateFile(file: FileEntity) = appDao.updateFile(file)

    suspend fun deleteFileById(id: Long) = appDao.deleteFileById(id)

    suspend fun insertTerminalHistory(history: TerminalHistoryEntity) = appDao.insertTerminalHistory(history)

    suspend fun clearTerminalHistory() = appDao.clearTerminalHistory()
}
