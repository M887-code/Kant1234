package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FileEntity
import com.example.data.ProjectEntity
import com.example.viewmodel.LineType
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.TerminalLine
import kotlinx.coroutines.launch

// Custom Theme Colors for CodeTerminal (Neon slate / Space)
val SpaceBg = Color(0xFF0F121A)
val CardBg = Color(0xFF161B22)
val TerminalGreen = Color(0xFF00FF66)
val IDEBlue = Color(0xFF58A6FF)
val TextLight = Color(0xFFE6EDF2)
val MutedGrey = Color(0xFF8B949E)
val AccentPink = Color(0xFFFF79C6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val projectFiles by viewModel.projectFiles.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    // Create New Project State
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newProjectLang by remember { mutableStateOf("Web") }

    // Create New File State
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SpaceBg,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "💻 Developer Workspace",
                        style = MaterialTheme.typography.titleLarge,
                        color = IDEBlue,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = Color.White.copy(0.1f))

                    // Project Selector
                    Text("Select Active Project", color = MutedGrey, style = MaterialTheme.typography.bodySmall)
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Button(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("project_selector_btn")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeProject?.name ?: "No Projects",
                                    color = TextLight,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MutedGrey)
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(CardBg).width(260.dp)
                        ) {
                            allProjects.forEach { proj ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(proj.name, color = TextLight, fontWeight = FontWeight.Bold)
                                            Text(proj.language, color = MutedGrey, style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectProject(proj)
                                        dropdownExpanded = false
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(0.1f))
                            DropdownMenuItem(
                                text = { Text("+ Create New Project", color = TerminalGreen) },
                                onClick = {
                                    showCreateProjectDialog = true
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(0.1f))

                    // Workspace Files
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Files", color = MutedGrey, style = MaterialTheme.typography.bodyMedium)
                        if (activeProject != null) {
                            IconButton(onClick = { showCreateFileDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "New File", tint = TerminalGreen)
                            }
                        }
                    }

                    if (activeProject == null) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Create a project to start coding.", color = MutedGrey, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(projectFiles) { file ->
                                val isSelected = selectedFile?.id == file.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent)
                                        .clickable {
                                            viewModel.selectFile(file)
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (file.language) {
                                        "html", "css", "js" -> Icons.Default.List
                                        "python", "kotlin" -> Icons.Default.Edit
                                        else -> Icons.Default.Home
                                    }
                                    Icon(
                                        icon,
                                        contentDescription = "file",
                                        tint = if (isSelected) IDEBlue else TextLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = file.path,
                                        color = if (isSelected) IDEBlue else TextLight,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteFile(file) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Project Actions
                        Button(
                            onClick = {
                                viewModel.deleteActiveProject()
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Current Project", color = Color.Red)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("CodeTerminal", color = TextLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = activeProject?.let { "${it.name} (${it.language})" } ?: "No Active Workspace",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGrey
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Workspace Manager", tint = TextLight)
                        }
                    },
                    actions = {
                        if (activeTab == 0 && selectedFile != null) {
                            IconButton(
                                onClick = {
                                    viewModel.saveActiveFile()
                                    Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = TerminalGreen)
                            }
                        }
                        IconButton(onClick = { viewModel.clearTerminal() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Terminal", tint = TextLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceBg)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SpaceBg,
                    contentColor = TextLight
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { viewModel.activeTab.value = 0 },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Editor") },
                        label = { Text("IDE Editor") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IDEBlue,
                            unselectedIconColor = MutedGrey,
                            indicatorColor = IDEBlue.copy(0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { viewModel.activeTab.value = 1 },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Terminal") },
                        label = { Text("Terminal") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TerminalGreen,
                            unselectedIconColor = MutedGrey,
                            indicatorColor = TerminalGreen.copy(0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { viewModel.activeTab.value = 2 },
                        icon = { Icon(Icons.Default.Build, contentDescription = "APK Builder") },
                        label = { Text("APK Builder") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentPink,
                            unselectedIconColor = MutedGrey,
                            indicatorColor = AccentPink.copy(0.15f)
                        )
                    )
                }
            },
            containerColor = SpaceBg
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(SpaceBg)
            ) {
                when (activeTab) {
                    0 -> EditorTab(viewModel)
                    1 -> TerminalTab(viewModel)
                    2 -> ApkBuilderTab(viewModel)
                }
            }
        }
    }

    // CREATE PROJECT DIALOG
    if (showCreateProjectDialog) {
        AlertDialog(
            onDismissRequest = { showCreateProjectDialog = false },
            title = { Text("Create New Project", color = TextLight, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MutedGrey,
                            focusedBorderColor = IDEBlue,
                            focusedLabelColor = IDEBlue
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_project_name_input")
                    )

                    Text("Programming Stack / Target:", color = MutedGrey, style = MaterialTheme.typography.bodySmall)
                    
                    val languages = listOf("Web", "Python", "Kotlin", "NodeJS")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { lang ->
                            val isSel = newProjectLang == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) IDEBlue else CardBg)
                                    .clickable { newProjectLang = lang }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lang, color = TextLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.createProject(newProjectName, newProjectLang)
                            showCreateProjectDialog = false
                            newProjectName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IDEBlue)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProjectDialog = false }) {
                    Text("Cancel", color = MutedGrey)
                }
            },
            containerColor = CardBg
        )
    }

    // CREATE FILE DIALOG
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create New File", color = TextLight) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter relative path (e.g. index.html, src/main.py):", color = MutedGrey)
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name / Path") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MutedGrey,
                            focusedBorderColor = IDEBlue,
                            focusedLabelColor = IDEBlue
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createFile(newFileName)
                            showCreateFileDialog = false
                            newFileName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IDEBlue)
                ) {
                    Text("Create File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel", color = MutedGrey)
                }
            },
            containerColor = CardBg
        )
    }
}

// --- TAB 1: CODE EDITOR ---
@Composable
fun EditorTab(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val editorText by viewModel.editorText.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val fontSize by viewModel.editorFontSize.collectAsStateWithLifecycle()
    val isSearchVisible by viewModel.isSearchVisible.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val replaceQuery by viewModel.replaceQuery.collectAsStateWithLifecycle()

    if (selectedFile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Folder", modifier = Modifier.size(64.dp), tint = MutedGrey)
                Text("No file open.", style = MaterialTheme.typography.titleMedium, color = TextLight)
                Text("Open the Sidebar (top-left menu) and choose a project file to begin editing code.", color = MutedGrey, textAlign = TextAlign.Center)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Toolbar (Undo, Redo, Save Status, Font scaling, Search toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Unsaved badge & file name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedFile?.path ?: "",
                    color = IDEBlue,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                if (hasUnsavedChanges) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Yellow)
                    )
                }
            }

            // Navigation Undo/Redo/Search Actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.undo() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Undo", tint = TextLight, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.redo() }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Redo", tint = TextLight, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.isSearchVisible.value = !isSearchVisible }) {
                    Icon(Icons.Default.Search, contentDescription = "Find", tint = if (isSearchVisible) IDEBlue else TextLight, modifier = Modifier.size(18.dp))
                }
                // Font Size Increments
                IconButton(onClick = { viewModel.editorFontSize.value = (fontSize - 1).coerceAtLeast(10f) }) {
                    Text("A-", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(onClick = { viewModel.editorFontSize.value = (fontSize + 1).coerceAtMost(28f) }) {
                    Text("A+", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Search Panel
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        label = { Text("Search term") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MutedGrey,
                            focusedBorderColor = IDEBlue,
                            focusedLabelColor = IDEBlue,
                            unfocusedLabelColor = MutedGrey,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { viewModel.replaceQuery.value = it },
                        label = { Text("Replace with") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MutedGrey,
                            focusedBorderColor = IDEBlue,
                            focusedLabelColor = IDEBlue,
                            unfocusedLabelColor = MutedGrey,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
                Button(
                    onClick = {
                        val fullText = editorText
                        if (searchQuery.isNotEmpty()) {
                            val replaced = fullText.replace(searchQuery, replaceQuery)
                            viewModel.onEditorTextChange(replaced)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IDEBlue),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Replace All")
                }
            }
        }

        // Editor Body (Lines & Text Grid)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SpaceBg)
        ) {
            // Line numbers display
            val lines = editorText.split("\n")
            val lineCount = lines.size
            
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .background(CardBg.copy(0.5f))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (num in 1..lineCount) {
                    Text(
                        text = num.toString(),
                        color = MutedGrey.copy(0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        style = TextStyle(lineHeight = (fontSize * 1.35f).sp)
                    )
                }
            }

            // Real editable TextField using VisualTransformation for Syntax Highlighting!
            val visualTransformation = VisualTransformation { text ->
                TransformedText(
                    getSyntaxHighlighter(text.text, selectedFile?.language ?: "text"),
                    OffsetMapping.Identity
                )
            }

            BasicTextField(
                value = editorText,
                onValueChange = { viewModel.onEditorTextChange(it) },
                textStyle = TextStyle(
                    color = TextLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.35f).sp
                ),
                cursorBrush = SolidColor(TerminalGreen),
                visualTransformation = visualTransformation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .testTag("code_editor_field")
            )
        }
    }
}

// --- TAB 2: TERMUX TERMINAL ---
@Composable
fun TerminalTab(viewModel: MainViewModel) {
    val terminalLines = viewModel.terminalLines
    val currentDir by viewModel.currentDirectory.collectAsStateWithLifecycle()
    val terminalInput by viewModel.terminalInput.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll terminal on new inputs
    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(terminalLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Console Screen Logs
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(terminalLines) { line ->
                val color = when (line.type) {
                    LineType.INPUT -> IDEBlue
                    LineType.ERROR -> Color.Red
                    LineType.SUCCESS -> TerminalGreen
                    LineType.OUTPUT -> TextLight
                }
                Text(
                    text = line.text,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Pinned Terminal Quick Keys Toolbar (Like Termux custom panel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val keys = listOf("ESC", "CTRL", "ALT", "TAB", "/", "-", "|", "UP", "CLR", "pkg")
            keys.forEach { key ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(0.1f))
                        .clickable {
                            when (key) {
                                "UP" -> {
                                    // Recycle last command
                                    viewModel.terminalInput.value = "python main.py"
                                }
                                "CLR" -> viewModel.clearTerminal()
                                "TAB" -> {
                                    viewModel.terminalInput.value += "  "
                                }
                                "pkg" -> {
                                    viewModel.terminalInput.value = "pkg install "
                                }
                                else -> {
                                    viewModel.terminalInput.value += key.lowercase()
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Prompt Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "u0_a254@termux:${currentDir.replace("~/", "")}$ ",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            BasicTextField(
                value = terminalInput,
                onValueChange = { viewModel.terminalInput.value = it },
                textStyle = TextStyle(
                    color = TextLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(TerminalGreen),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (terminalInput.isNotBlank()) {
                            viewModel.executeTerminalCommand(terminalInput)
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field")
            )
        }
    }
}

// --- TAB 3: APK BUILDER HUB ---
@Composable
fun ApkBuilderTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val isCompiling by viewModel.isCompiling.collectAsStateWithLifecycle()
    val compileProgress by viewModel.compileProgress.collectAsStateWithLifecycle()
    val compiledApkFile by viewModel.compiledApkFile.collectAsStateWithLifecycle()
    val compileLogs = viewModel.compileLogs

    // Config states from VM
    val appName by viewModel.apkAppName.collectAsStateWithLifecycle()
    val packageName by viewModel.apkPackageName.collectAsStateWithLifecycle()
    val versionName by viewModel.apkVersionName.collectAsStateWithLifecycle()
    val versionCode by viewModel.apkVersionCode.collectAsStateWithLifecycle()
    val appIconColor by viewModel.apkAppIconColor.collectAsStateWithLifecycle()
    val selectedPermissions by viewModel.apkSelectedPermissions.collectAsStateWithLifecycle()

    if (activeProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Please select or create a project first to access the APK compilation engine.", color = MutedGrey, textAlign = TextAlign.Center)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "📦 APK Manifest settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentPink,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = appName,
                        onValueChange = { viewModel.apkAppName.value = it },
                        label = { Text("Application Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MutedGrey,
                            focusedBorderColor = AccentPink,
                            focusedLabelColor = AccentPink,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("apk_name_input")
                    )

                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { viewModel.apkPackageName.value = it },
                        label = { Text("Android Package ID") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MutedGrey,
                            focusedBorderColor = AccentPink,
                            focusedLabelColor = AccentPink,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("apk_pkg_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { viewModel.apkVersionName.value = it },
                            label = { Text("Version Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MutedGrey,
                                focusedBorderColor = AccentPink,
                                focusedLabelColor = AccentPink,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = versionCode,
                            onValueChange = { viewModel.apkVersionCode.value = it },
                            label = { Text("Version Code") },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MutedGrey,
                                focusedBorderColor = AccentPink,
                                focusedLabelColor = AccentPink,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Icon Generator Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "🎨 Adaptive Launcher Icon Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentPink,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom Live Adaptive Icon Preview Canvas
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(appIconColor), Color(appIconColor).copy(0.6f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Draws custom logo inside preview
                            Canvas(modifier = Modifier.size(40.dp)) {
                                drawCircle(
                                    color = Color.White.copy(0.15f),
                                    radius = size.minDimension / 1.5f
                                )
                            }
                            Icon(
                                Icons.Default.Build,
                                contentDescription = "Launcher Icon preview",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                              )
                        }

                        // Colors Palette picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Brand Theme Accent", style = MaterialTheme.typography.bodySmall, color = MutedGrey)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val colors = listOf(0xFF2196F3, 0xFF4CAF50, 0xFFFF5722, 0xFFE91E63, 0xFF9C27B0, 0xFF607D8B)
                                colors.forEach { c ->
                                    val isSel = appIconColor == c.toInt()
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .clickable { viewModel.apkAppIconColor.value = c.toInt() }
                                            .then(
                                                if (isSel) Modifier.background(Color.White.copy(0.4f)) else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSel) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Permissions Request Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🛡️ Target App Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentPink,
                        fontWeight = FontWeight.Bold
                    )

                    val perms = listOf(
                        Pair("android.permission.INTERNET", "Internet Connection Access"),
                        Pair("android.permission.CAMERA", "Camera Hardware Capture"),
                        Pair("android.permission.WRITE_EXTERNAL_STORAGE", "Local Storage Access"),
                        Pair("android.permission.RECORD_AUDIO", "Microphone Voice Capture"),
                        Pair("android.permission.ACCESS_FINE_LOCATION", "GPS Satellite Location")
                    )

                    perms.forEach { (perm, desc) ->
                        val hasP = selectedPermissions.contains(perm)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (hasP) {
                                        viewModel.apkSelectedPermissions.value = selectedPermissions - perm
                                    } else {
                                        viewModel.apkSelectedPermissions.value = selectedPermissions + perm
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasP,
                                onCheckedChange = { checked ->
                                    if (checked == true) {
                                        viewModel.apkSelectedPermissions.value = selectedPermissions + perm
                                    } else {
                                        viewModel.apkSelectedPermissions.value = selectedPermissions - perm
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AccentPink)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(perm.substringAfterLast("."), color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(desc, color = MutedGrey, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Action Compile Trigger
        item {
            Button(
                onClick = { viewModel.compileAndBuildApk(context) },
                enabled = !isCompiling,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("compile_apk_btn")
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "compile")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compile & Package App (APK)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Real-Time Compiler log display
        if (isCompiling || compileLogs.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gradle Build Console Log", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            if (isCompiling) {
                                Text("${(compileProgress * 100).toInt()}%", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                        
                        if (isCompiling) {
                            LinearProgressIndicator(
                                progress = { compileProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = TerminalGreen,
                                trackColor = Color.DarkGray
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            compileLogs.forEach { log ->
                                Text(
                                    text = log,
                                    color = if (log.startsWith(">")) IDEBlue else TextLight,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export APK Card
        if (compiledApkFile != null && !isCompiling) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalGreen.copy(0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(true)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "success", tint = TerminalGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("BUILD SUCCESSFUL", color = TerminalGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Text(
                            text = "Created installable APK package containing the assets and code files compiled on your device.",
                            color = TextLight,
                            fontSize = 13.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.shareCompiledApk(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export & Install APK", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom simple regex-based syntax parser
fun getSyntaxHighlighter(text: String, language: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        if (text.isEmpty()) return@buildAnnotatedString

        // Bright high contrast neon theme colors
        val keywordColor = Color(0xFFFF79C6) // Pink
        val stringColor = Color(0xFFF1FA8C) // Yellow
        val commentColor = Color(0xFF6272A4) // Gray-blue
        val numberColor = Color(0xFFBD93F9) // Purple
        val tagColor = Color(0xFF8BE9FD) // Cyan
        val functionColor = Color(0xFF50FA7B) // Green
        
        try {
            when (language.lowercase()) {
                "html" -> {
                    // Match opening/closing tags
                    "</?[a-zA-Z0-9\\-]+>?".toRegex().findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = tagColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                    }
                    // Attributes
                    "[a-zA-Z0-9\\-]+=".toRegex().findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = keywordColor), match.range.first, match.range.last)
                    }
                    // Double strings
                    "\"[^\"]*\"".toRegex().findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
                    }
                }
                "python", "kotlin", "js" -> {
                    val keywords = when (language.lowercase()) {
                        "python" -> listOf("def", "import", "from", "for", "in", "if", "else", "print", "range", "while", "return", "as")
                        "kotlin" -> listOf("package", "import", "class", "fun", "val", "var", "if", "else", "for", "while", "remember", "mutableStateOf", "return")
                        else -> listOf("const", "let", "var", "function", "if", "else", "for", "while", "setInterval", "document", "window", "return", "console", "log")
                    }

                    // Comments
                    val commentPattern = if (language == "python") "#.*".toRegex() else "//.*".toRegex()
                    commentPattern.findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                    }

                    // Double strings
                    "\"[^\"]*\"".toRegex().findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
                    }

                    // Single strings
                    "'[^']*'".toRegex().findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
                    }

                    // Keywords matching
                    keywords.forEach { word ->
                        "\\b$word\\b".toRegex().findAll(text).forEach { match ->
                            addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
                        }
                    }

                    // Function calls
                    "\\b[a-zA-Z0-9_]+\\(".toRegex().findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = functionColor), match.range.first, match.range.last)
                    }

                    // Numbers
                    "\\b\\d+(\\.\\d+)?\\b".toRegex().findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback gracefully on parsing errors
        }
    }
}
