package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())

        // Ensure database is populated on start
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    // Reactive list of steps from Room
    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Progress State (percentage & raw counts)
    val progressState: StateFlow<ProgressInfo> = tasks.combine(MutableStateFlow(0)) { taskList, _ ->
        val total = taskList.size
        val completed = taskList.count { it.isCompleted }
        val percentage = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
        ProgressInfo(completed = completed, total = total, percentage = percentage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressInfo(0, 9, 0f)
    )

    // Sim Terminal logs state
    private val _terminalLogs = MutableStateFlow<List<TerminalLogLine>>(
        listOf(
            TerminalLogLine("SYSTEM STATUS: ONLINE • ALL SYSTEMS READY", isSystem = true),
            TerminalLogLine("RexDevCyber Sim-Terminal v1.0.4", isSystem = true),
            TerminalLogLine("Type 'help' to view available setup commands.", isSystem = true),
            TerminalLogLine("rx-developer@cyber-x:~ $ ", isPrompt = true)
        )
    )
    val terminalLogs: StateFlow<List<TerminalLogLine>> = _terminalLogs.asStateFlow()

    // Current terminal input
    private val _terminalInput = MutableStateFlow("")
    val terminalInput: StateFlow<String> = _terminalInput.asStateFlow()

    // Status variables
    private val _isTerminalBusy = MutableStateFlow(false)
    val isTerminalBusy: StateFlow<Boolean> = _isTerminalBusy.asStateFlow()

    private val _nodeVersionActive = MutableStateFlow("v20.12.0")
    val nodeVersionActive: StateFlow<String> = _nodeVersionActive.asStateFlow()

    private val _localServerRunning = MutableStateFlow(false)
    val localServerRunning: StateFlow<Boolean> = _localServerRunning.asStateFlow()

    private val _firebaseLoggedInUser = MutableStateFlow<String?>(null)
    val firebaseLoggedInUser: StateFlow<String?> = _firebaseLoggedInUser.asStateFlow()

    // Interactive Config Builders State
    private val _crowdinProjectId = MutableStateFlow("rexdevcyber-x")
    val crowdinProjectId: StateFlow<String> = _crowdinProjectId.asStateFlow()

    private val _crowdinApiToken = MutableStateFlow("CROWDIN_PERSONAL_TOKEN")
    val crowdinApiToken: StateFlow<String> = _crowdinApiToken.asStateFlow()

    private val _crowdinSource = MutableStateFlow("/src/locales/th.json")
    val crowdinSource: StateFlow<String> = _crowdinSource.asStateFlow()

    private val _crowdinTranslation = MutableStateFlow("/src/locales/%three_letters_code%.json")
    val crowdinTranslation: StateFlow<String> = _crowdinTranslation.asStateFlow()

    private val _firebasePublicDir = MutableStateFlow("dist")
    val firebasePublicDir: StateFlow<String> = _firebasePublicDir.asStateFlow()

    private val _firebaseSinglePageApp = MutableStateFlow(true)
    val firebaseSinglePageApp: StateFlow<Boolean> = _firebaseSinglePageApp.asStateFlow()

    private val _firebaseIgnores = MutableStateFlow("firebase.json, **/.*, **/node_modules/**")
    val firebaseIgnores: StateFlow<String> = _firebaseIgnores.asStateFlow()

    // Actions
    fun updateTask(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskCompletion(id, isCompleted)
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.resetAllTasks()
            _localServerRunning.value = false
            _firebaseLoggedInUser.value = null
            addTerminalLine("Progress databases and server configurations were reset successfully.", isSystem = true)
            addPromptLine()
        }
    }

    fun updateTerminalInput(input: String) {
        _terminalInput.value = input
    }

    // Config updating helpers
    fun updateCrowdinProjectId(value: String) { _crowdinProjectId.value = value }
    fun updateCrowdinApiToken(value: String) { _crowdinApiToken.value = value }
    fun updateCrowdinSource(value: String) { _crowdinSource.value = value }
    fun updateCrowdinTranslation(value: String) { _crowdinTranslation.value = value }
    fun updateFirebasePublicDir(value: String) { _firebasePublicDir.value = value }
    fun updateFirebaseSinglePageApp(value: Boolean) { _firebaseSinglePageApp.value = value }
    fun updateFirebaseIgnores(value: String) { _firebaseIgnores.value = value }

    fun addTerminalLine(text: String, isError: Boolean = false, isSystem: Boolean = false, isLight: Boolean = false) {
        val current = _terminalLogs.value.toMutableList()
        current.add(TerminalLogLine(text, isError, isSystem, isLight))
        _terminalLogs.value = current
    }

    private fun addPromptLine() {
        val current = _terminalLogs.value.toMutableList()
        current.add(TerminalLogLine("rx-developer@cyber-x:~ $ ", isPrompt = true))
        _terminalLogs.value = current
    }

    // Main interaction handler
    fun triggerTerminalCommand(commandString: String) {
        if (_isTerminalBusy.value) return
        val cmd = commandString.trim()
        if (cmd.isEmpty()) return

        // Replace the prompt line with input text
        val logs = _terminalLogs.value.toMutableList()
        if (logs.isNotEmpty() && logs.last().isPrompt) {
            logs.removeAt(logs.size - 1)
        }
        logs.add(TerminalLogLine("rx-developer@cyber-x:~ $ $cmd", isLight = true))
        _terminalLogs.value = logs

        _terminalInput.value = ""
        _isTerminalBusy.value = true

        viewModelScope.launch {
            when (cmd.lowercase().trim()) {
                "help" -> {
                    delay(200)
                    addTerminalLine("Available Setup Commands:")
                    addTerminalLine("  node -v                            Check Node.js version")
                    addTerminalLine("  npm -v                             Check npm package manager version")
                    addTerminalLine("  npm install                        Install all packages and dependencies")
                    addTerminalLine("  npm run dev                        Starts local Vite server on port 3000")
                    addTerminalLine("  npm install -g firebase-tools      Installs Firebase suite CLI tools globally")
                    addTerminalLine("  firebase login                     Authenticates CLI to google developer cloud")
                    addTerminalLine("  firebase init                      Sets folder web deployment config parameters")
                    addTerminalLine("  npm run build                      Compiles static, compressed folder assets")
                    addTerminalLine("  firebase deploy                    Publishes compiled dist/ client directly to global CDN")
                    addTerminalLine("  clear                              Clears all logs in Sim-Terminal")
                    addTerminalLine("  reset                              Resets progress databases")
                }
                "clear" -> {
                    _terminalLogs.value = emptyList()
                }
                "reset" -> {
                    delay(300)
                    repository.resetAllTasks()
                    _localServerRunning.value = false
                    _firebaseLoggedInUser.value = null
                    addTerminalLine("Database & mock host status reset successfully.", isSystem = true)
                }
                "node -v" -> {
                    delay(300)
                    addTerminalLine(_nodeVersionActive.value)
                    repository.updateTaskCompletion(2, true) // Check off node version review
                }
                "npm -v" -> {
                    delay(300)
                    addTerminalLine("10.5.0")
                    repository.updateTaskCompletion(2, true) // Check off node/npm verification
                }
                "npm install" -> {
                    addTerminalLine("npm WARN config global `\u002D\u002Dglobal`, `\u002D\u002Dlocal` are deprecated. Use `\u002D\u002Dlocation=global` instead.")
                    addTerminalLine("npm db: fetch metadata...")
                    delay(400)
                    addTerminalLine("resolving dependencies...")
                    delay(400)
                    addTerminalLine("fetch [======================= ] 84% - barprogress-01290")
                    delay(400)
                    addTerminalLine("added 522 packages, and audited 523 packages in 1.2s")
                    addTerminalLine("found 0 vulnerabilities", isSystem = true)
                    repository.updateTaskCompletion(3, true) // Check off npm install
                }
                "npm run dev" -> {
                    addTerminalLine("> rexdevcyber-x@1.0.4 dev")
                    addTerminalLine("> vite --port 3000")
                    delay(500)
                    addTerminalLine("  VITE v5.2.10  ready in 324 ms", isSystem = true)
                    addTerminalLine("  \u279C  Local:   http://localhost:3000/", isSystem = true)
                    addTerminalLine("  \u279C  Network: use --host to expose", isSystem = true)
                    addTerminalLine("  \u279C  press h + enter to show help")
                    _localServerRunning.value = true
                    repository.updateTaskCompletion(4, true) // Check off npm run dev
                }
                "npm install -g firebase-tools" -> {
                    addTerminalLine("npm WARN global-install CLI package detected...")
                    delay(400)
                    addTerminalLine("downloading packages: @firebase/tools-suite-cli")
                    delay(500)
                    addTerminalLine("+ firebase-tools@13.11.2", isSystem = true)
                    addTerminalLine("added 134 packages from 88 contributors in 0.9s")
                    repository.updateTaskCompletion(7, true) // Marks initial deploy setup step
                }
                "firebase login" -> {
                    addTerminalLine("i  Firebase login checking credentials...")
                    delay(400)
                    addTerminalLine("? Allow Firebase to collect CLI and Emulator Suite usage? (Y/n) Yes")
                    delay(500)
                    addTerminalLine("Opening browser authenticating link...")
                    delay(600)
                    addTerminalLine("\u2714  Success! Logged in as jokraxfluk@gmail.com", isSystem = true)
                    _firebaseLoggedInUser.value = "jokraxfluk@gmail.com"
                    repository.updateTaskCompletion(7, true) // Check off firebase login
                }
                "firebase init" -> {
                    addTerminalLine("     \u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D-")
                    addTerminalLine("    (   | |__   __\u002D-  |__   _ _(_)__ _  __ _ ___ )")
                    addTerminalLine("    (  _| ' \u005C \u002D_ \u2215 _` | ' \u005C \u2215 _` \u2215 _` \u2215_ \u2215_` /  _ \u2215\u002D- )")
                    addTerminalLine("     \u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D\u002D-")
                    delay(400)
                    addTerminalLine("? Which Firebase features do you want to set up? (Press Space to select)")
                    addTerminalLine(" \u25EF Firestore: Configure rules and index files")
                    addTerminalLine(" \u2714 Hosting: Configure files for Firebase Hosting and (optionally) set up GitHub Actions", isSystem = true)
                    delay(500)
                    addTerminalLine("? What do you want to use as your public directory? dist")
                    addTerminalLine("? Configure as a single-page app (rewrite all urls to /index.html)? Yes")
                    delay(400)
                    addTerminalLine("\u2714  Firebase initialization complete!", isSystem = true)
                    repository.updateTaskCompletion(7, true) // Completes part of deployment setup
                }
                "npm run build" -> {
                    addTerminalLine("> rexdevcyber-x@1.0.4 build")
                    addTerminalLine("> tsc && vite build")
                    delay(400)
                    addTerminalLine("vite v5.2.10 building for production...")
                    delay(500)
                    addTerminalLine("transforming (24) assets...")
                    delay(400)
                    addTerminalLine("dist/index.html                     0.84 kB \u2502 gzip:  0.42 kB")
                    addTerminalLine("dist/assets/index-D9K823fS.css     42.12 kB \u2502 gzip:  8.94 kB")
                    addTerminalLine("dist/assets/index-CpO2sWnC.js     212.44 kB \u2502 gzip: 65.40 kB")
                    addTerminalLine("\u2713 built in 1.08s", isSystem = true)
                    repository.updateTaskCompletion(8, true) // Check off build
                }
                "firebase deploy" -> {
                    addTerminalLine("=== Deploying to 'rexdevcyber-x'...")
                    delay(400)
                    addTerminalLine("i  deploying hosting")
                    addTerminalLine("i  hosting[rexdevcyber-x]: beginning deploy for 3 files...")
                    delay(500)
                    addTerminalLine("i  hosting[rexdevcyber-x]: uploading files [3/3] (100%)")
                    delay(400)
                    addTerminalLine("\u2714  hosting[rexdevcyber-x]: file upload complete", isSystem = true)
                    addTerminalLine("i  hosting[rexdevcyber-x]: finalizing version...")
                    addTerminalLine("\u2714  hosting[rexdevcyber-x]: release launched successfully!", isSystem = true)
                    delay(400)
                    addTerminalLine("\u2714  Deploy complete!", isSystem = true)
                    addTerminalLine("")
                    addTerminalLine("Project Console: https://console.firebase.google.com/project/rexdevcyber-x/overview")
                    addTerminalLine("Hosting URL:     https://rexdevcyber-x.web.app", isSystem = true)
                    repository.updateTaskCompletion(9, true) // Check off deploy
                }
                else -> {
                    delay(150)
                    addTerminalLine("Command not found; '$cmd'. Type 'help' to see active parameters.", isError = true)
                }
            }

            // Always finish with a prompt unless the user selected clear
            if (cmd.lowercase() != "clear") {
                addPromptLine()
            }
            _isTerminalBusy.value = false
        }
    }

    // Real-Time Collaboration States
    private val _isCollabActive = MutableStateFlow(false)
    val isCollabActive: StateFlow<Boolean> = _isCollabActive.asStateFlow()

    private val _collaborators = MutableStateFlow<List<Collaborator>>(emptyList())
    val collaborators: StateFlow<List<Collaborator>> = _collaborators.asStateFlow()

    private val _sheetsLogs = MutableStateFlow<List<SheetsCollabLog>>(emptyList())
    val sheetsLogs: StateFlow<List<SheetsCollabLog>> = _sheetsLogs.asStateFlow()

    private val _localSelectedCell = MutableStateFlow<Pair<Int, Int>?>(null)
    val localSelectedCell: StateFlow<Pair<Int, Int>?> = _localSelectedCell.asStateFlow()

    private var collabJob: kotlinx.coroutines.Job? = null
    private var nextLogId = 1L

    fun addCollabLog(message: String) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val timeString = sdf.format(java.util.Date())
        val current = _sheetsLogs.value.toMutableList()
        current.add(SheetsCollabLog(nextLogId++, message, timeString))
        _sheetsLogs.value = current.takeLast(50) // keep a max of 50 log lines
    }

    fun startCollabSession() {
        if (_isCollabActive.value) return
        _isCollabActive.value = true
        addCollabLog("ระบบกูเกิลชีตส์แชร์: เริ่มต้นการทำงานร่วมกันแบบเรียลไทม์")
        addCollabLog("คุณ (jokraxfluk@gmail.com) ได้เข้าร่วมสเปรดชีตแชร์")
        
        _collaborators.value = listOf(
            Collaborator("Somchai L.", "somchai.l@rexdev.io", "#3B82F6", 1, 2, isEditing = false, "เปิดแก้ไขไฟล์ชีต"),
            Collaborator("Cyber Admin", "admin.cyber@rexdev.io", "#EF4444", 3, 4, isEditing = false, "ตรวจงานสเปก"),
            Collaborator("Sheets Bot", "bot-auto@rexdev.io", "#10B981", 6, 4, isEditing = false, "สแตนด์บายซิงค์อัตโนมัติ")
        )
        
        _sheetsLogs.value = _sheetsLogs.value.toMutableList().apply {
            add(SheetsCollabLog(nextLogId++, "Somchai L. เข้าร่วมชีตการทำงานร่วมกัน", "14:30:10"))
            add(SheetsCollabLog(nextLogId++, "Cyber Admin แสตนด์บายควบคุมระบบ", "14:30:12"))
            add(SheetsCollabLog(nextLogId++, "Sheets Bot เริ่มต้นการจัดคิวซิงค์ระดับข้ามเครือข่าย", "14:30:15"))
        }

        collabJob = viewModelScope.launch {
            while (_isCollabActive.value) {
                delay((3000L..6000L).random())
                val activeList = _collaborators.value.toMutableList()
                if (activeList.isEmpty()) continue
                
                val randomIndex = activeList.indices.random()
                val targetCollab = activeList[randomIndex]
                
                // Decide action: 0,1,2 = Move cursor, 3 = Move cursor & edit
                val actionType = (0..3).random()
                if (actionType == 3) {
                    // Start editing
                    val randomRow = (0..8).random()
                    val randomCol = (2..4).random() // Title, Desc, Status
                    
                    activeList[randomIndex] = targetCollab.copy(
                        row = randomRow,
                        col = randomCol,
                        isEditing = true,
                        lastAction = "กำลังพิมพ์แก้ไขช่อง (${('A'.code + randomCol).toChar()}${randomRow + 1})..."
                    )
                    _collaborators.value = activeList
                    
                    delay(1500) // typing simulation delay
                    
                    val currentTasks = tasks.value
                    if (randomRow < currentTasks.size) {
                        val task = currentTasks[randomRow]
                        val collabName = targetCollab.name
                        
                        when (randomCol) {
                            2 -> {
                                val currentTitle = task.title
                                val newTitle = if (currentTitle.contains(" (แก้ไขโดย ")) {
                                    currentTitle.substringBefore(" (แก้ไขโดย ") + " (แก้ไขโดย $collabName)"
                                } else {
                                    "$currentTitle (แก้ไขโดย $collabName)"
                                }
                                repository.updateTaskDetails(task.id, newTitle, task.description, task.isCompleted)
                                addCollabLog("$collabName แก้ไขชื่อขั้นตอน #${task.id} เป็น \"$newTitle\"")
                            }
                            3 -> {
                                val currentDesc = task.description
                                val suffix = " [อัปเดตผ่านคลาวด์แชร์]"
                                val newDesc = if (currentDesc.endsWith(suffix)) {
                                    currentDesc.removeSuffix(suffix)
                                } else {
                                    "$currentDesc$suffix"
                                }
                                repository.updateTaskDetails(task.id, task.title, newDesc, task.isCompleted)
                                addCollabLog("$collabName อัปเดตรายละเอียดสเต็ป #${task.id}")
                            }
                            4 -> {
                                val currentCompleted = task.isCompleted
                                val newCompleted = !currentCompleted
                                repository.updateTaskCompletion(task.id, newCompleted)
                                val statusText = if (newCompleted) "COMPLETED" else "INCOMPLETE"
                                addCollabLog("$collabName สลับแผ่นสถานะหลัก #${task.id} -> $statusText")
                                addTerminalLine("COLLAB SYNC: $collabName toggled step #${task.id} status to $statusText from Google Sheet.", isSystem = true)
                            }
                        }
                    }
                    
                    val freshCollab = _collaborators.value.toMutableList()
                    if (randomIndex < freshCollab.size) {
                        freshCollab[randomIndex] = freshCollab[randomIndex].copy(
                            isEditing = false,
                            lastAction = "แก้ไขคอลัมน์คลาสสิกเสร็จสิ้น"
                        )
                        _collaborators.value = freshCollab
                    }
                } else {
                    // Move cursor
                    val randomRow = (0..8).random()
                    val randomCol = (0..5).random()
                    activeList[randomIndex] = targetCollab.copy(
                        row = randomRow,
                        col = randomCol,
                        lastAction = "ย้ายเคอร์เซอร์ไปตำแหน่งแผ่นงาน (${('A'.code + randomCol).toChar()}${randomRow + 1})"
                    )
                    _collaborators.value = activeList
                    
                    if ((0..2).random() == 0) {
                        val cellName = "${('A'.code + randomCol).toChar()}${randomRow + 1}"
                        addCollabLog("${targetCollab.name} ย้ายเคอร์เซอร์ไปตำแหน่ง $cellName")
                    }
                }
            }
        }
    }

    fun stopCollabSession() {
        _isCollabActive.value = false
        collabJob?.cancel()
        collabJob = null
        addCollabLog("ระบบกูเกิลชีตส์แชร์: สิ้นสุด/ตัดการเชื่อมต่อเรียลไทม์")
    }

    fun updateLocalSelection(row: Int, col: Int) {
        _localSelectedCell.value = Pair(row, col)
        val cellName = "${('A'.code + col).toChar()}${row + 1}"
        addCollabLog("คุณ เลือกช่องเกรดงานที่ $cellName")
    }

    fun clearLocalSelection() {
        _localSelectedCell.value = null
    }

    fun editLocalCell(row: Int, col: Int, value: String) {
        viewModelScope.launch {
            val currentTasks = tasks.value
            if (row < currentTasks.size) {
                val task = currentTasks[row]
                when (col) {
                    2 -> {
                        repository.updateTaskDetails(task.id, value, task.description, task.isCompleted)
                        addCollabLog("คุณ (jokraxfluk@gmail.com) เปลี่ยนชื่อกิจกรรม #${task.id} เป็น \"$value\"")
                        addTerminalLine("EDIT SUCCESS: Local user updated step #${task.id} title on sheet to '$value'.", isSystem = true)
                    }
                    3 -> {
                        repository.updateTaskDetails(task.id, task.title, value, task.isCompleted)
                        addCollabLog("คุณ (jokraxfluk@gmail.com) แก้ไขคำอธิบาย #${task.id} เป็น \"$value\"")
                        addTerminalLine("EDIT SUCCESS: Local user updated step #${task.id} description on sheet.", isSystem = true)
                    }
                    4 -> {
                        val isComp = value.trim().uppercase() == "COMPLETED" || value.trim().lowercase() == "true" || value.trim() == "1"
                        repository.updateTaskCompletion(task.id, isComp)
                        val statusText = if (isComp) "COMPLETED" else "INCOMPLETE"
                        addCollabLog("คุณ (jokraxfluk@gmail.com) สลับสถานะกิจกรรม #${task.id} -> $statusText")
                        addTerminalLine("EDIT SUCCESS: Local user updated step #${task.id} status on sheet to $statusText.", isSystem = true)
                    }
                }
            }
        }
    }
}

// Data Classes used for views
data class ProgressInfo(
    val completed: Int,
    val total: Int,
    val percentage: Float
)

data class TerminalLogLine(
    val text: String,
    val isError: Boolean = false,
    val isSystem: Boolean = false,
    val isLight: Boolean = false,
    val isPrompt: Boolean = false
)

data class Collaborator(
    val name: String,
    val email: String,
    val colorHex: String,
    val row: Int,
    val col: Int,
    val isEditing: Boolean = false,
    val lastAction: String = ""
)

data class SheetsCollabLog(
    val id: Long,
    val message: String,
    val timestamp: String
)
