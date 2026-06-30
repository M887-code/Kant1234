package com.example.viewmodel

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.FileEntity
import com.example.data.ProjectEntity
import com.example.data.TerminalHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT
)

enum class LineType {
    INPUT, OUTPUT, ERROR, SUCCESS
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // Projects list
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project state
    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    // Files in active project
    private val _projectFiles = MutableStateFlow<List<FileEntity>>(emptyList())
    val projectFiles: StateFlow<List<FileEntity>> = _projectFiles.asStateFlow()

    // Currently open file in the editor
    private val _selectedFile = MutableStateFlow<FileEntity?>(null)
    val selectedFile: StateFlow<FileEntity?> = _selectedFile.asStateFlow()

    // Editor content buffers
    var editorText = MutableStateFlow("")
    val hasUnsavedChanges = MutableStateFlow(false)
    val editorFontSize = MutableStateFlow(14f)

    // Undo/Redo Stacks
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private var isApplyingUndoRedo = false

    // Search / Replace States
    val searchQuery = MutableStateFlow("")
    val replaceQuery = MutableStateFlow("")
    val isSearchVisible = MutableStateFlow(false)

    // Terminal History Logs
    val terminalLines = mutableStateListOf<TerminalLine>()
    val terminalInput = MutableStateFlow("")
    val currentDirectory = MutableStateFlow("~/") // Root of workspace

    // Simulated installed packages (Termux mirrors)
    val installedPackages = MutableStateFlow(setOf("bash", "coreutils", "pkg", "git", "clang"))

    // Active Tab in App: 0 = Project/Editor, 1 = Terminal, 2 = APK Builder
    val activeTab = MutableStateFlow(0)

    // APK Compilation State
    val isCompiling = MutableStateFlow(false)
    val compileProgress = MutableStateFlow(0f)
    val compileLogs = mutableStateListOf<String>()
    val compiledApkFile = MutableStateFlow<File?>(null)

    // Custom compiler configurations
    val apkAppName = MutableStateFlow("My Web Game")
    val apkPackageName = MutableStateFlow("com.mycompany.webgame")
    val apkVersionName = MutableStateFlow("1.0.0")
    val apkVersionCode = MutableStateFlow("1")
    val apkAppIconColor = MutableStateFlow(0xFF2196F3.toInt()) // Blue default
    val apkSelectedPermissions = MutableStateFlow(setOf("android.permission.INTERNET"))

    init {
        // Observe projects. If empty, pre-populate default templates
        viewModelScope.launch {
            allProjects.collect { projects ->
                if (projects.isEmpty()) {
                    createDefaultTemplates()
                } else if (_activeProject.value == null) {
                    // Set first project as active automatically
                    selectProject(projects.first())
                }
            }
        }

        // Initialize Terminal Greeting
        clearTerminal()
        terminalLines.add(TerminalLine("Welcome to CodeTerminal v2.5!", LineType.SUCCESS))
        terminalLines.add(TerminalLine("A Termux-like shell running on Android architecture.", LineType.OUTPUT))
        terminalLines.add(TerminalLine("Type 'help' to see list of supported commands.", LineType.OUTPUT))
        terminalLines.add(TerminalLine("", LineType.OUTPUT))
    }

    private suspend fun createDefaultTemplates() {
        // Template 1: HTML5 Game (Web App)
        val p1 = ProjectEntity(
            name = "Space Invaders Lite",
            language = "Web",
            packageName = "com.aistudio.spaceinvaders",
            appIconColor = 0xFFFF5722.toInt() // Deep Orange
        )
        val p1Id = repository.insertProject(p1)
        repository.insertFile(FileEntity(
            projectId = p1Id,
            path = "index.html",
            content = """<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>
        body {
            margin: 0;
            background: #111;
            color: #fff;
            font-family: sans-serif;
            text-align: center;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            height: 100vh;
        }
        canvas {
            border: 2px solid #ff5722;
            background: #000;
            box-shadow: 0 0 20px rgba(255,87,34,0.3);
            max-width: 95vw;
            max-height: 70vh;
        }
        #ui {
            margin-top: 10px;
        }
        button {
            background: #ff5722;
            color: #fff;
            border: none;
            padding: 10px 20px;
            font-size: 16px;
            border-radius: 5px;
            cursor: pointer;
            margin: 5px;
        }
    </style>
</head>
<body>
    <h3>Space Invaders Lite</h3>
    <canvas id="gameCanvas" width="360" height="400"></canvas>
    <div id="ui">
        <button onclick="startGame()">Start Game</button>
        <button onclick="moveLeft()">Left</button>
        <button onclick="moveRight()">Right</button>
    </div>
    <script src="app.js"></script>
</body>
</html>""",
            language = "html"
        ))
        repository.insertFile(FileEntity(
            projectId = p1Id,
            path = "app.js",
            content = """// Simple Canvas Game Loop
const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");

let playerX = 160;
let bulletY = -10;
let bulletX = 0;
let score = 0;
let invaders = [];

function startGame() {
    score = 0;
    playerX = 160;
    bulletY = -10;
    invaders = [];
    for(let i=0; i<5; i++) {
        invaders.push({ x: 30 + i * 60, y: 50, active: true });
    }
    draw();
}

function moveLeft() {
    playerX = Math.max(10, playerX - 15);
    draw();
}

function moveRight() {
    playerX = Math.min(310, playerX + 15);
    draw();
}

// Simple loop
setInterval(() => {
    if (bulletY > 0) {
        bulletY -= 12;
        // Collision
        invaders.forEach(inv => {
            if (inv.active && Math.abs(bulletX - inv.x) < 20 && Math.abs(bulletY - inv.y) < 15) {
                inv.active = false;
                score += 10;
                bulletY = -10;
            }
        });
    } else if (Math.random() < 0.05) {
        // Fire bullet
        bulletX = playerX + 20;
        bulletY = 360;
    }
    
    // Move invaders down
    invaders.forEach(inv => {
        if(inv.active && Math.random() < 0.02) {
            inv.y += 5;
        }
    });

    draw();
}, 100);

function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Draw player
    ctx.fillStyle = "#ff5722";
    ctx.fillRect(playerX, 360, 40, 20);
    
    // Draw invaders
    ctx.fillStyle = "#4CAF50";
    invaders.forEach(inv => {
        if (inv.active) {
            ctx.beginPath();
            ctx.arc(inv.x, inv.y, 12, 0, Math.PI * 2);
            ctx.fill();
        }
    });
    
    // Draw bullet
    if (bulletY > 0) {
        ctx.fillStyle = "#FFFF00";
        ctx.fillRect(bulletX, bulletY, 4, 10);
    }
    
    // Draw UI
    ctx.fillStyle = "#fff";
    ctx.font = "14px Courier";
    ctx.fillText("Score: " + score, 10, 20);
}

startGame();""",
            language = "js"
        ))

        // Template 2: Python Script (Analytical)
        val p2 = ProjectEntity(
            name = "Python Calc Engine",
            language = "Python",
            packageName = "com.aistudio.pythoncalc",
            appIconColor = 0xFF4CAF50.toInt() // Green
        )
        val p2Id = repository.insertProject(p2)
        repository.insertFile(FileEntity(
            projectId = p2Id,
            path = "main.py",
            content = """# Interactive Python analytics tool
print("--- Python Dev Engine Loading ---")
user_name = "CodeTerminal Builder"
items = 4
price = 19.99

print("User Name:", user_name)
print("Analyzing items count...")

total = items * price
print("Total Price calculated:", total)

# Execute loop simulation
print("Initiating calculations loop:")
for i in range(5):
    step = i + 1
    factor = step * 12.5
    print("Step", step, "factor is:", factor)
    if factor > 30:
        print("  -> Factor threshold reached (> 30)")

print("Script complete. Well done!")
""",
            language = "python"
        ))

        // Template 3: Kotlin Compose Code (Kotlin APP)
        val p3 = ProjectEntity(
            name = "Android UI Demo",
            language = "Kotlin",
            packageName = "com.aistudio.composeapp",
            appIconColor = 0xFF3F51B5.toInt() // Indigo
        )
        val p3Id = repository.insertProject(p3)
        repository.insertFile(FileEntity(
            projectId = p3Id,
            path = "src/MainActivity.kt",
            content = """package com.example.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var count by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome to Compiled APK App!",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(text = "This app was created and compiled directly on an Android device using CodeTerminal.")
        
        Button(onClick = { count++ }) {
            Text("Clicks: ${'$'}count")
        }
    }
}""",
            language = "kotlin"
        ))
        repository.insertFile(FileEntity(
            projectId = p3Id,
            path = "AndroidManifest.xml",
            content = """<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.aistudio.composeapp">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:label="Android UI Demo"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>""",
            language = "xml"
        ))
    }

    fun selectProject(project: ProjectEntity) {
        _activeProject.value = project
        // Load files for this project
        viewModelScope.launch {
            repository.getFilesByProject(project.id).collect { files ->
                _projectFiles.value = files
                // Set default open file if none selected
                if (files.isNotEmpty() && (_selectedFile.value == null || _selectedFile.value?.projectId != project.id)) {
                    selectFile(files.firstOrNull { f -> f.path.endsWith(".html") || f.path.endsWith(".py") || f.path.endsWith(".kt") } ?: files.first())
                }
            }
        }
        // Sync APK Builder settings
        apkAppName.value = project.name
        apkPackageName.value = project.packageName
        apkAppIconColor.value = project.appIconColor
    }

    fun selectFile(file: FileEntity) {
        _selectedFile.value = file
        editorText.value = file.content
        hasUnsavedChanges.value = false
        undoStack.clear()
        redoStack.clear()
    }

    fun onEditorTextChange(newText: String) {
        if (isApplyingUndoRedo) return

        val oldText = editorText.value
        if (oldText != newText) {
            undoStack.add(oldText)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            editorText.value = newText
            hasUnsavedChanges.value = true
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            isApplyingUndoRedo = true
            val current = editorText.value
            redoStack.add(current)
            val previous = undoStack.removeAt(undoStack.size - 1)
            editorText.value = previous
            hasUnsavedChanges.value = true
            isApplyingUndoRedo = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isApplyingUndoRedo = true
            val current = editorText.value
            undoStack.add(current)
            val next = redoStack.removeAt(redoStack.size - 1)
            editorText.value = next
            hasUnsavedChanges.value = true
            isApplyingUndoRedo = false
        }
    }

    fun saveActiveFile() {
        val file = _selectedFile.value ?: return
        val currentText = editorText.value
        viewModelScope.launch {
            val updated = file.copy(content = currentText)
            repository.updateFile(updated)
            _selectedFile.value = updated
            hasUnsavedChanges.value = false
        }
    }

    fun createProject(name: String, language: String) {
        viewModelScope.launch {
            val appThemeColor = when (language) {
                "Web" -> 0xFFff5722.toInt() // Deep Orange
                "Python" -> 0xFF4caf50.toInt() // Green
                "Kotlin" -> 0xFF3f51b5.toInt() // Indigo
                else -> 0xFF607d8b.toInt() // Blue Grey
            }
            val pkg = "com.aistudio.${name.lowercase().replace(" ", "")}"
            val newProject = ProjectEntity(
                name = name,
                language = language,
                packageName = pkg,
                appIconColor = appThemeColor
            )
            val pId = repository.insertProject(newProject)
            
            // Add default main files
            when (language) {
                "Web" -> {
                    repository.insertFile(FileEntity(projectId = pId, path = "index.html", content = "<h1>Hello $name!</h1>\n<p>Start coding in HTML/CSS/JS</p>", language = "html"))
                    repository.insertFile(FileEntity(projectId = pId, path = "style.css", content = "body { font-family: sans-serif; background: #fafafa; }", language = "css"))
                    repository.insertFile(FileEntity(projectId = pId, path = "app.js", content = "console.log('App loaded');", language = "js"))
                }
                "Python" -> {
                    repository.insertFile(FileEntity(projectId = pId, path = "main.py", content = "print('Hello from Python!')\nfor i in range(3):\n    print('Index:', i)", language = "python"))
                }
                "Kotlin" -> {
                    repository.insertFile(FileEntity(projectId = pId, path = "src/MainActivity.kt", content = "package $pkg\n\nfun main() {\n    println(\"Hello from Kotlin!\")\n}", language = "kotlin"))
                }
                "NodeJS" -> {
                    repository.insertFile(FileEntity(projectId = pId, path = "index.js", content = "console.log('NodeJS Server running on localhost:3000');\nfor (let i = 1; i <= 3; i++) {\n    console.log('Tick:', i);\n}", language = "js"))
                }
            }
            // Trigger selection of new project
            val insertedProject = repository.getProjectById(pId)
            if (insertedProject != null) {
                selectProject(insertedProject)
            }
        }
    }

    fun deleteActiveProject() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            repository.deleteProject(proj.id)
            _activeProject.value = null
            _selectedFile.value = null
            _projectFiles.value = emptyList()
        }
    }

    fun createFile(fileName: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val extension = fileName.substringAfterLast(".", "").lowercase()
            val language = when (extension) {
                "html" -> "html"
                "css" -> "css"
                "js" -> "js"
                "py" -> "python"
                "kt" -> "kotlin"
                "json" -> "json"
                "sh" -> "sh"
                else -> "text"
            }
            val newFile = FileEntity(
                projectId = proj.id,
                path = fileName,
                content = "",
                language = language
            )
            repository.insertFile(newFile)
        }
    }

    fun deleteFile(file: FileEntity) {
        viewModelScope.launch {
            repository.deleteFileById(file.id)
            if (_selectedFile.value?.id == file.id) {
                _selectedFile.value = null
                editorText.value = ""
            }
        }
    }

    // --- TERMINAL SIMULATION CORE (Termux Engine) ---
    fun clearTerminal() {
        terminalLines.clear()
    }

    fun executeTerminalCommand(fullCommand: String) {
        if (fullCommand.isBlank()) return
        
        terminalLines.add(TerminalLine("$ ${currentDirectory.value.replace("~/", "")} $fullCommand", LineType.INPUT))
        terminalInput.value = ""

        val parts = fullCommand.trim().split("\\s+".toRegex())
        val command = parts[0].lowercase()
        val args = parts.drop(1)

        viewModelScope.launch {
            when (command) {
                "help" -> showHelp()
                "clear" -> clearTerminal()
                "whoami" -> terminalLines.add(TerminalLine("u0_a254", LineType.OUTPUT))
                "uname" -> {
                    if (args.contains("-a")) {
                        terminalLines.add(TerminalLine("Linux termux 5.10.43-android13-9-g3be31e-ab904323 #1 SMP PREEMPT Mon Jun 29 15:00:00 UTC 2026 aarch64 Android", LineType.OUTPUT))
                    } else {
                        terminalLines.add(TerminalLine("Linux", LineType.OUTPUT))
                    }
                }
                "pwd" -> terminalLines.add(TerminalLine("/data/data/com.termux/files/home/${currentDirectory.value.replace("~/", "")}", LineType.OUTPUT))
                "ls" -> terminalLs()
                "cd" -> terminalCd(args.firstOrNull())
                "mkdir" -> terminalMkdir(args.firstOrNull())
                "touch" -> terminalTouch(args.firstOrNull())
                "cat" -> terminalCat(args.firstOrNull())
                "rm" -> terminalRm(args.firstOrNull())
                "echo" -> terminalLines.add(TerminalLine(args.joinToString(" "), LineType.OUTPUT))
                "pkg", "apt" -> terminalPkg(args)
                "neofetch" -> terminalNeofetch()
                "python", "python3" -> terminalPython(args.firstOrNull())
                "node" -> terminalNode(args.firstOrNull())
                "sh", "bash" -> terminalSh(args.firstOrNull())
                "./" -> terminalExecuteActive(args.firstOrNull() ?: "")
                "gcc", "g++" -> terminalGcc(args)
                "git" -> terminalGit(args)
                "gradle", "gradlew" -> terminalGradle(args)
                else -> {
                    // Check if it's a relative execution like `./run.sh`
                    if (fullCommand.startsWith("./")) {
                        val scriptName = fullCommand.substring(2)
                        terminalSh(scriptName)
                    } else {
                        terminalLines.add(TerminalLine("bash: command not found: $command. Type 'help' for available options.", LineType.ERROR))
                    }
                }
            }
            // Auto scroll is handled in compose UI
        }
    }

    private fun showHelp() {
        val helpText = """
            Available Termux commands:
            ------------------------------------------
            ls                  List files in current directory
            cd [dir]            Change directory
            pwd                 Print working directory
            mkdir [name]        Create directory
            touch [name]        Create blank file
            cat [file]          View file content
            rm [file]           Delete file
            echo [text]         Display custom text
            clear               Clear shell screen
            whoami              Display active terminal user
            uname -a            Display Android Linux system info
            neofetch            Visual system dashboard
            pkg install [pkg]   Install: python, nodejs, git, clang
            python [file].py    Execute Python file
            node [file].js      Execute JavaScript file
            gcc [file].c        Compile C code
            git clone [url]     Clone git repository (e.g. game, util)
            gradle assemble     Simulate building Android APK
            ------------------------------------------
        """.trimIndent()
        helpText.split("\n").forEach {
            terminalLines.add(TerminalLine(it, LineType.OUTPUT))
        }
    }

    private fun terminalLs() {
        val proj = _activeProject.value
        if (proj == null) {
            terminalLines.add(TerminalLine("No active workspace project loaded. Create one first.", LineType.ERROR))
            return
        }

        val relativePathPrefix = currentDirectory.value.replace("~/", "")
        val files = _projectFiles.value

        val entries = mutableSetOf<String>()
        files.forEach { file ->
            val path = file.path
            if (path.startsWith(relativePathPrefix)) {
                val subPath = path.substring(relativePathPrefix.length)
                val segments = subPath.split("/").filter { it.isNotEmpty() }
                if (segments.isNotEmpty()) {
                    val entry = segments[0]
                    if (segments.size > 1) {
                        entries.add("$entry/") // It's a directory
                    } else {
                        entries.add(entry) // It's a file
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            terminalLines.add(TerminalLine("(empty directory)", LineType.OUTPUT))
        } else {
            terminalLines.add(TerminalLine(entries.sorted().joinToString("   "), LineType.SUCCESS))
        }
    }

    private fun terminalCd(dir: String?) {
        if (dir == null || dir == "~" || dir == "/") {
            currentDirectory.value = "~/"
            return
        }
        if (dir == "..") {
            val current = currentDirectory.value
            if (current == "~/") return
            val parts = current.split("/").filter { it.isNotEmpty() }
            if (parts.size <= 1) {
                currentDirectory.value = "~/"
            } else {
                currentDirectory.value = "~/" + parts.dropLast(1).joinToString("/") + "/"
            }
            return
        }

        // Verify dir exists virtually in our files list
        val targetSubpath = currentDirectory.value.replace("~/", "") + dir + "/"
        val exists = _projectFiles.value.any { it.path.startsWith(targetSubpath) }
        if (exists) {
            currentDirectory.value = "~/" + targetSubpath
        } else {
            terminalLines.add(TerminalLine("cd: no such file or directory: $dir", LineType.ERROR))
        }
    }

    private suspend fun terminalMkdir(dir: String?) {
        if (dir == null) {
            terminalLines.add(TerminalLine("mkdir: missing operand", LineType.ERROR))
            return
        }
        val proj = _activeProject.value ?: return
        val newDirPath = currentDirectory.value.replace("~/", "") + dir + "/.keep"
        repository.insertFile(FileEntity(
            projectId = proj.id,
            path = newDirPath,
            content = "",
            language = "text"
        ))
        terminalLines.add(TerminalLine("Directory $dir created.", LineType.OUTPUT))
    }

    private suspend fun terminalTouch(file: String?) {
        if (file == null) {
            terminalLines.add(TerminalLine("touch: missing file operand", LineType.ERROR))
            return
        }
        val proj = _activeProject.value ?: return
        val filePath = currentDirectory.value.replace("~/", "") + file
        repository.insertFile(FileEntity(
            projectId = proj.id,
            path = filePath,
            content = "",
            language = "text"
        ))
        terminalLines.add(TerminalLine("Created empty file $file.", LineType.OUTPUT))
    }

    private suspend fun terminalCat(file: String?) {
        if (file == null) {
            terminalLines.add(TerminalLine("cat: missing file operand", LineType.ERROR))
            return
        }
        val proj = _activeProject.value ?: return
        val filePath = currentDirectory.value.replace("~/", "") + file
        val found = repository.getFileByPath(proj.id, filePath)
        if (found != null) {
            found.content.split("\n").forEach {
                terminalLines.add(TerminalLine(it, LineType.OUTPUT))
            }
        } else {
            terminalLines.add(TerminalLine("cat: $file: No such file or directory", LineType.ERROR))
        }
    }

    private suspend fun terminalRm(file: String?) {
        if (file == null) {
            terminalLines.add(TerminalLine("rm: missing operand", LineType.ERROR))
            return
        }
        val proj = _activeProject.value ?: return
        val filePath = currentDirectory.value.replace("~/", "") + file
        val found = repository.getFileByPath(proj.id, filePath)
        if (found != null) {
            repository.deleteFileById(found.id)
            terminalLines.add(TerminalLine("Removed file $file.", LineType.OUTPUT))
        } else {
            terminalLines.add(TerminalLine("rm: cannot remove '$file': No such file", LineType.ERROR))
        }
    }

    private suspend fun terminalPkg(args: List<String>) {
        if (args.isEmpty()) {
            terminalLines.add(TerminalLine("pkg: package manager. Usage: pkg install [package_name]", LineType.OUTPUT))
            return
        }
        val sub = args[0]
        if (sub == "install") {
            if (args.size < 2) {
                terminalLines.add(TerminalLine("pkg install: package name required.", LineType.ERROR))
                return
            }
            val targetPkg = args[1].lowercase()
            val listSupported = listOf("python", "nodejs", "clang", "git", "neofetch")
            if (!listSupported.contains(targetPkg)) {
                terminalLines.add(TerminalLine("pkg install: package '$targetPkg' not found in Termux repositories.", LineType.ERROR))
                return
            }

            terminalLines.add(TerminalLine("Updating Termux repository mirrors...", LineType.OUTPUT))
            delay(1000)
            terminalLines.add(TerminalLine("Downloading package metadata...", LineType.OUTPUT))
            delay(1000)
            terminalLines.add(TerminalLine("Downloading binary: $targetPkg...", LineType.OUTPUT))
            
            // Render a cool simulated progress bar
            for (progress in 10..100 step 20) {
                val bar = "=".repeat(progress / 5) + ">" + " ".repeat((100 - progress) / 5)
                terminalLines.add(TerminalLine("[$bar] $progress% (ETA: 0s)", LineType.OUTPUT))
                delay(400)
            }
            
            terminalLines.add(TerminalLine("Unpacking resources...", LineType.OUTPUT))
            delay(1000)
            terminalLines.add(TerminalLine("Configuring environment and symlinks...", LineType.OUTPUT))
            delay(800)

            installedPackages.value = installedPackages.value + targetPkg
            terminalLines.add(TerminalLine("Package $targetPkg installed successfully! Type '$targetPkg --help' or execute scripts.", LineType.SUCCESS))
        } else if (sub == "list") {
            terminalLines.add(TerminalLine("Installed packages:", LineType.SUCCESS))
            installedPackages.value.forEach {
                terminalLines.add(TerminalLine(" - $it", LineType.OUTPUT))
            }
        } else {
            terminalLines.add(TerminalLine("pkg: unknown action '$sub'.", LineType.ERROR))
        }
    }

    private fun terminalNeofetch() {
        if (!installedPackages.value.contains("neofetch")) {
            terminalLines.add(TerminalLine("bash: neofetch: command not found. Install it first: pkg install neofetch", LineType.ERROR))
            return
        }

        val neofetchText = """
         _  _     u0_a254@termux
       _/ \/ \_   --------------
      / \_/\_/ \  OS: Termux on Android 13
      \_/\_/\_/   Kernel: 5.10.43-android13-9-g3be31e
        \_/\_/    Uptime: 4 hours, 32 mins
                  Packages: ${installedPackages.value.size} (pkg)
                  Shell: bash 5.2
                  Terminal: termux-emulator
                  CPU: Snapdragon 8 Gen 2 (8)
                  Memory: 6.88 GB / 12.00 GB
                  Workspace: Active Code Editor
                  Device: Mobile Android Host
        """.trimIndent()
        neofetchText.split("\n").forEach {
            terminalLines.add(TerminalLine(it, LineType.OUTPUT))
        }
    }

    private suspend fun terminalPython(file: String?) {
        if (!installedPackages.value.contains("python")) {
            terminalLines.add(TerminalLine("bash: python: command not found. Run 'pkg install python' to install Python.", LineType.ERROR))
            return
        }
        if (file == null) {
            terminalLines.add(TerminalLine("Python 3.11.2 (default, Jun 29 2026)\n[Clang 14.0.7] on android\nType \"help\" for more info.", LineType.OUTPUT))
            return
        }

        val proj = _activeProject.value ?: return
        val filePath = currentDirectory.value.replace("~/", "") + file
        val found = repository.getFileByPath(proj.id, filePath)
        if (found == null) {
            terminalLines.add(TerminalLine("python: can't open file '$file': No such file or directory", LineType.ERROR))
            return
        }

        terminalLines.add(TerminalLine("python3 $file", LineType.OUTPUT))
        // Execute python interpreter
        runPythonInterpreter(found.content)
    }

    private suspend fun runPythonInterpreter(code: String) {
        val lines = code.split("\n")
        val variables = mutableMapOf<String, Any>()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#")) {
                i++
                continue
            }

            try {
                // Parse loop
                if (line.startsWith("for ") && line.endsWith(":")) {
                    // Extract iterator and limit
                    val loopHeader = line.substring(4, line.length - 1) // e.g. "i in range(5)"
                    val parts = loopHeader.split(" in ")
                    val loopVarName = parts[0].trim()
                    val rangePart = parts[1].trim()
                    
                    val limit = if (rangePart.startsWith("range(") && rangePart.endsWith(")")) {
                        val limitStr = rangePart.substring(6, rangePart.length - 1).trim()
                        variables[limitStr]?.toString()?.toIntOrNull() ?: limitStr.toIntOrNull() ?: 1
                    } else {
                        1
                    }

                    // Find loop body (lines following with larger indentation)
                    val loopBody = mutableListOf<String>()
                    var j = i + 1
                    while (j < lines.size) {
                        val nextLine = lines[j]
                        if (nextLine.isBlank()) {
                            loopBody.add("")
                            j++
                        } else {
                            // Python indentation check
                            val leadingSpaces = nextLine.takeWhile { it == ' ' || it == '\t' }.length
                            if (leadingSpaces > 0) {
                                loopBody.add(nextLine.trim())
                                j++
                            } else {
                                break
                            }
                        }
                    }

                    // Run the loop body!
                    for (count in 0 until limit) {
                        variables[loopVarName] = count
                        runPythonLines(loopBody, variables)
                    }
                    
                    i = j // Advance pointer past loop body
                    continue
                }

                // Parse standard statements
                executePythonSingleLine(line, variables)
            } catch (e: Exception) {
                terminalLines.add(TerminalLine("Python syntax error on line ${i+1}: ${e.message}", LineType.ERROR))
            }
            i++
        }
    }

    private fun runPythonLines(lines: List<String>, variables: MutableMap<String, Any>) {
        lines.forEach { line ->
            if (line.isNotEmpty() && !line.startsWith("#")) {
                executePythonSingleLine(line, variables)
            }
        }
    }

    private fun executePythonSingleLine(line: String, variables: MutableMap<String, Any>) {
        if (line.startsWith("print(") && line.endsWith(")")) {
            val contentStr = line.substring(6, line.length - 1)
            // Parse arguments (split by commas but respect strings)
            val printOutput = evaluatePythonPrint(contentStr, variables)
            terminalLines.add(TerminalLine(printOutput, LineType.OUTPUT))
        } else if (line.contains("=")) {
            // Assignment
            val parts = line.split("=", limit = 2)
            val varName = parts[0].trim()
            val expr = parts[1].trim()
            val value = evaluatePythonExpression(expr, variables)
            variables[varName] = value
        } else if (line.startsWith("if ") && line.endsWith(":")) {
            // Simply log conditional hit
            terminalLines.add(TerminalLine("Checking condition: ${line.substring(3, line.length-1)} -> True", LineType.OUTPUT))
        }
    }

    private fun evaluatePythonPrint(argStr: String, variables: Map<String, Any>): String {
        // Basic parser for comma-separated expressions
        val builder = StringBuilder()
        var currentToken = StringBuilder()
        var inString = false
        var stringChar = ' '
        
        var i = 0
        while (i < argStr.length) {
            val char = argStr[i]
            if ((char == '"' || char == '\'') && (i == 0 || argStr[i-1] != '\\')) {
                if (inString) {
                    if (char == stringChar) {
                        inString = false
                    } else {
                        currentToken.append(char)
                    }
                } else {
                    inString = true
                    stringChar = char
                }
            } else if (char == ',' && !inString) {
                // Flush token
                val t = currentToken.toString().trim()
                builder.append(resolvePythonToken(t, variables)).append(" ")
                currentToken = StringBuilder()
            } else {
                currentToken.append(char)
            }
            i++
        }
        if (currentToken.isNotEmpty()) {
            builder.append(resolvePythonToken(currentToken.toString().trim(), variables))
        }
        return builder.toString()
    }

    private fun resolvePythonToken(token: String, variables: Map<String, Any>): String {
        if (token.startsWith("f\"") || token.startsWith("f'")) {
            // Formatted string interpolation
            var str = token.substring(2, token.length - 1)
            val regex = "\\{([^}]+)}".toRegex()
            return regex.replace(str) { match ->
                val vName = match.groupValues[1].trim()
                variables[vName]?.toString() ?: ""
            }
        }
        if (variables.containsKey(token)) {
            return variables[token].toString()
        }
        // Arithmetic evaluation
        val arithmetic = evaluatePythonExpression(token, variables)
        return arithmetic.toString()
    }

    private fun evaluatePythonExpression(expr: String, variables: Map<String, Any>): Any {
        if (expr.startsWith("\"") && expr.endsWith("\"")) return expr.substring(1, expr.length - 1)
        if (expr.startsWith("'") && expr.endsWith("'")) return expr.substring(1, expr.length - 1)
        
        val floatVal = expr.toFloatOrNull()
        if (floatVal != null) {
            if (floatVal == floatVal.toInt().toFloat()) return floatVal.toInt()
            return floatVal
        }

        // Basic arithmetic solver (+, -, *, /)
        val ops = listOf("+", "-", "*", "/")
        for (op in ops) {
            if (expr.contains(op)) {
                val index = expr.lastIndexOf(op)
                val left = expr.substring(0, index).trim()
                val right = expr.substring(index + 1).trim()
                val leftVal = evaluatePythonExpression(left, variables).toString().toDoubleOrNull() ?: 0.0
                val rightVal = evaluatePythonExpression(right, variables).toString().toDoubleOrNull() ?: 0.0
                return when (op) {
                    "+" -> if (leftVal % 1 == 0.0 && rightVal % 1 == 0.0) (leftVal.toInt() + rightVal.toInt()) else (leftVal + rightVal)
                    "-" -> if (leftVal % 1 == 0.0 && rightVal % 1 == 0.0) (leftVal.toInt() - rightVal.toInt()) else (leftVal - rightVal)
                    "*" -> if (leftVal % 1 == 0.0 && rightVal % 1 == 0.0) (leftVal.toInt() * rightVal.toInt()) else (leftVal * rightVal)
                    "/" -> if (rightVal != 0.0) leftVal / rightVal else "ZeroDivisionError"
                    else -> 0
                }
            }
        }

        if (variables.containsKey(expr)) {
            return variables[expr]!!
        }

        return expr
    }

    private suspend fun terminalNode(file: String?) {
        if (!installedPackages.value.contains("nodejs")) {
            terminalLines.add(TerminalLine("bash: node: command not found. Install Node first: pkg install nodejs", LineType.ERROR))
            return
        }
        if (file == null) {
            terminalLines.add(TerminalLine("Welcome to Node.js v18.14.0.\nType \".help\" for more information.", LineType.OUTPUT))
            return
        }
        val proj = _activeProject.value ?: return
        val filePath = currentDirectory.value.replace("~/", "") + file
        val found = repository.getFileByPath(proj.id, filePath)
        if (found == null) {
            terminalLines.add(TerminalLine("node: cannot open file '$file': No such file", LineType.ERROR))
            return
        }

        terminalLines.add(TerminalLine("node $file", LineType.OUTPUT))
        // Basic node console interpreter
        found.content.split("\n").forEach { line ->
            val l = line.trim()
            if (l.startsWith("console.log(") && l.endsWith(");")) {
                val valStr = l.substring(12, l.length - 2)
                if (valStr.startsWith("'") || valStr.startsWith("\"")) {
                    terminalLines.add(TerminalLine(valStr.substring(1, valStr.length - 1), LineType.OUTPUT))
                } else {
                    terminalLines.add(TerminalLine(valStr, LineType.OUTPUT))
                }
            } else if (l.startsWith("console.log(") && l.endsWith(")")) {
                val valStr = l.substring(12, l.length - 1)
                if (valStr.startsWith("'") || valStr.startsWith("\"")) {
                    terminalLines.add(TerminalLine(valStr.substring(1, valStr.length - 1), LineType.OUTPUT))
                } else {
                    terminalLines.add(TerminalLine(valStr, LineType.OUTPUT))
                }
            }
        }
    }

    private suspend fun terminalSh(file: String?) {
        if (file == null) {
            terminalLines.add(TerminalLine("bash: script name required.", LineType.ERROR))
            return
        }
        val proj = _activeProject.value ?: return
        val filePath = currentDirectory.value.replace("~/", "") + file
        val found = repository.getFileByPath(proj.id, filePath)
        if (found == null) {
            terminalLines.add(TerminalLine("bash: $file: No such file or directory", LineType.ERROR))
            return
        }

        // Execute script lines sequentially as individual commands!
        found.content.split("\n").forEach { line ->
            val l = line.trim()
            if (l.isNotEmpty() && !l.startsWith("#")) {
                executeTerminalCommand(l)
                delay(300)
            }
        }
    }

    private suspend fun terminalExecuteActive(file: String) {
        if (file == "a.out") {
            terminalLines.add(TerminalLine("Executing ./a.out compiled binary:", LineType.SUCCESS))
            terminalLines.add(TerminalLine("Hello World! Program completed with exit status 0.", LineType.OUTPUT))
        } else {
            terminalSh(file)
        }
    }

    private suspend fun terminalGcc(args: List<String>) {
        if (!installedPackages.value.contains("clang")) {
            terminalLines.add(TerminalLine("bash: gcc: command not found. Install compiler toolchain: pkg install clang", LineType.ERROR))
            return
        }
        if (args.isEmpty()) {
            terminalLines.add(TerminalLine("gcc: error: no input files", LineType.ERROR))
            return
        }
        val sourceFile = args[0]
        terminalLines.add(TerminalLine("Compiling $sourceFile using clang (GCC wrapper) version 14.0...", LineType.OUTPUT))
        delay(1200)
        terminalLines.add(TerminalLine("Pre-processing step completed.", LineType.OUTPUT))
        delay(800)
        terminalLines.add(TerminalLine("Linking objects to a.out binary...", LineType.OUTPUT))
        delay(1000)
        
        // Add a virtual executable to project files
        val proj = _activeProject.value ?: return
        repository.insertFile(FileEntity(
            projectId = proj.id,
            path = "a.out",
            content = "# Binary compiled by GCC",
            language = "sh"
        ))
        
        terminalLines.add(TerminalLine("BUILD SUCCESSFUL. Executable generated: ./a.out", LineType.SUCCESS))
    }

    private suspend fun terminalGit(args: List<String>) {
        if (!installedPackages.value.contains("git")) {
            terminalLines.add(TerminalLine("bash: git: command not found. Install git: pkg install git", LineType.ERROR))
            return
        }
        if (args.isEmpty()) {
            terminalLines.add(TerminalLine("git: Usage: git clone [repository_url]", LineType.OUTPUT))
            return
        }
        val sub = args[0]
        if (sub == "clone") {
            if (args.size < 2) {
                terminalLines.add(TerminalLine("git clone: Repository URL is required.", LineType.ERROR))
                return
            }
            val repoUrl = args[1]
            terminalLines.add(TerminalLine("Cloning into '$repoUrl'...", LineType.OUTPUT))
            delay(1000)
            terminalLines.add(TerminalLine("remote: Enumerating objects: 12, done.", LineType.OUTPUT))
            delay(800)
            terminalLines.add(TerminalLine("remote: Counting objects: 100% (12/12), done.", LineType.OUTPUT))
            delay(500)
            terminalLines.add(TerminalLine("Receiving objects: 100% (12/12), 48.24 KiB | 1.2 MiB/s, done.", LineType.OUTPUT))
            delay(600)
            terminalLines.add(TerminalLine("Resolving deltas: 100% (2/2), done.", LineType.OUTPUT))
            
            // Create cloned files virtually
            val proj = _activeProject.value ?: return
            repository.insertFile(FileEntity(
                projectId = proj.id,
                path = "cloned_app.py",
                content = """# Cloned script from Git
print("Hello from cloned Python utility!")
import math
print("Pi is calculated as:", math.pi)
""",
                language = "python"
            ))
            terminalLines.add(TerminalLine("Cloned successfully. New file added: cloned_app.py", LineType.SUCCESS))
        } else {
            terminalLines.add(TerminalLine("git: unknown command '$sub'. Only git clone is modeled.", LineType.ERROR))
        }
    }

    private suspend fun terminalGradle(args: List<String>) {
        terminalLines.add(TerminalLine("Triggering Android app build pipeline via Gradle...", LineType.OUTPUT))
        delay(800)
        triggerApkCompilationFlow()
    }


    // --- APK COMPILER ENGINE CORE ---
    fun compileAndBuildApk(context: Context) {
        viewModelScope.launch {
            triggerApkCompilationFlow(context)
        }
    }

    private suspend fun triggerApkCompilationFlow(context: Context? = null) {
        isCompiling.value = true
        compileProgress.value = 0f
        compileLogs.clear()
        compiledApkFile.value = null

        val tasks = listOf(
            Pair("> Task :app:preBuild", 300L),
            Pair("> Task :app:compileDebugAidl NO-SOURCE", 200L),
            Pair("> Task :app:generateDebugBuildConfig", 400L),
            Pair("> Task :app:mergeDebugResources", 600L),
            Pair("> Task :app:processDebugManifest", 500L),
            Pair("> Task :app:javaPreCompileDebug", 300L),
            Pair("> Task :app:compileDebugKotlin (analyzing Kotlin sources...)", 1200L),
            Pair("> Task :app:dexBuilderDebug (assembling classes.dex bytecode)", 1000L),
            Pair("> Task :app:mergeProjectDexDebug", 600L),
            Pair("> Task :app:packageDebug (compressing app files & assets)", 1400L),
            Pair("> Task :app:signDebugApk (signing package with test/debug keys)", 800L),
            Pair("> Task :app:zipalignDebugApk (optimizing archive alignment)", 600L)
        )

        val totalTasks = tasks.size.toFloat()
        
        for (index in tasks.indices) {
            val (taskLog, duration) = tasks[index]
            compileLogs.add(taskLog)
            if (taskLog.contains("compileDebugKotlin")) {
                compileLogs.add("  - Compiling active source files...")
                _projectFiles.value.forEach { file ->
                    compileLogs.add("    -> Compiled: ${file.path}")
                    delay(200)
                }
                compileLogs.add("  - Kotlin classes compiled successfully.")
            }
            if (taskLog.contains("packageDebug")) {
                compileLogs.add("  - Packaging web resources into assets/www...")
                _projectFiles.value.filter { it.language in listOf("html", "css", "js") }.forEach { file ->
                    compileLogs.add("    -> Bundling asset: assets/www/${file.path}")
                    delay(200)
                }
            }
            delay(duration)
            compileProgress.value = (index + 1) / totalTasks
        }

        // Build a REAL physical ZIP file structured as an APK file in cache directory!
        if (context != null) {
            val apkFile = buildRealApkZip(context)
            compiledApkFile.value = apkFile
            compileLogs.add("  - Created package file: ${apkFile.name} (${apkFile.length() / 1024} KB)")
        }

        compileLogs.add("BUILD SUCCESSFUL in ${(1..5).random()}s")
        isCompiling.value = false
    }

    private suspend fun buildRealApkZip(context: Context): File = withContext(Dispatchers.IO) {
        // Output file in cache
        val outputApkName = "${apkAppName.value.replace(" ", "_")}_release.apk"
        val apkFile = File(context.cacheDir, outputApkName)
        if (apkFile.exists()) apkFile.delete()

        ZipOutputStream(FileOutputStream(apkFile)).use { zos ->
            // 1. AndroidManifest.xml (Simulated manifest text)
            val manifestContent = """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="${apkPackageName.value}">
                    ${apkSelectedPermissions.value.joinToString("\n") { "    <uses-permission android:name=\"$it\" />" }}
                    <application
                        android:label="${apkAppName.value}"
                        android:icon="@mipmap/ic_launcher">
                        <activity android:name=".MainActivity" android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()
            
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(manifestContent.toByteArray())
            zos.closeEntry()

            // 2. Dummy classes.dex
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("DEX\nCompiled with CodeTerminal Mobile JVM D8 Engine".toByteArray())
            zos.closeEntry()

            // 3. Dummy resources.arsc
            zos.putNextEntry(ZipEntry("resources.arsc"))
            zos.write("ARSC\nCodeTerminal Package Resources Header Table".toByteArray())
            zos.closeEntry()

            // 4. Project source files in assets/www/
            _projectFiles.value.forEach { file ->
                zos.putNextEntry(ZipEntry("assets/www/${file.path}"))
                zos.write(file.content.toByteArray())
                zos.closeEntry()
            }
        }
        apkFile
    }

    fun shareCompiledApk(context: Context) {
        val apk = compiledApkFile.value ?: return
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Built APK"))
    }
}
