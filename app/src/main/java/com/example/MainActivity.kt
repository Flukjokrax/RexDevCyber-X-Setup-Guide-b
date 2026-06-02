package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontStyle
import com.example.data.TaskEntity
import com.example.ui.MainViewModel
import com.example.ui.ProgressInfo
import com.example.ui.TerminalLogLine
import com.example.ui.Collaborator
import com.example.ui.SheetsCollabLog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = DenseBg
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // State bindings
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val progressInfo by viewModel.progressState.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val terminalInput by viewModel.terminalInput.collectAsStateWithLifecycle()
    val isTerminalBusy by viewModel.isTerminalBusy.collectAsStateWithLifecycle()
    val localServerRunning by viewModel.localServerRunning.collectAsStateWithLifecycle()

    // Screen tab selection (0 = Setup, 1 = CLI, 2 = Configs, 3 = Team)
    var selectedBottomTab by remember { mutableStateOf(0) }

    // Walkthrough selected step details dialog state
    var selectedWalkthroughPhase by remember { mutableStateOf<Int?>(null) }

    // Google Sheets Synchronize simulation dialog state
    var isSyncingWithSheets by remember { mutableStateOf(false) }
    var syncPhaseText by remember { mutableStateOf("") }
    var syncSuccessful by remember { mutableStateOf(false) }

    // Google Sheets Real-Time Collaboration States
    val isCollabActive by viewModel.isCollabActive.collectAsStateWithLifecycle()
    val collaborators by viewModel.collaborators.collectAsStateWithLifecycle()
    val sheetsLogs by viewModel.sheetsLogs.collectAsStateWithLifecycle()
    val localSelectedCell by viewModel.localSelectedCell.collectAsStateWithLifecycle()
    var isSheetsCollabOpen by remember { mutableStateOf(false) }

    // Config Input States
    val crowdinId by viewModel.crowdinProjectId.collectAsStateWithLifecycle()
    val crowdinToken by viewModel.crowdinApiToken.collectAsStateWithLifecycle()
    val crowdinSource by viewModel.crowdinSource.collectAsStateWithLifecycle()
    val crowdinTranslation by viewModel.crowdinTranslation.collectAsStateWithLifecycle()

    val firebasePublicDir by viewModel.firebasePublicDir.collectAsStateWithLifecycle()
    val firebaseSinglePageApp by viewModel.firebaseSinglePageApp.collectAsStateWithLifecycle()
    val firebaseIgnores by viewModel.firebaseIgnores.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, DenseBorder))
            ) {
                // Setup Navigation Tab
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Setup Checklist") },
                    label = { Text("Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DensePrimary,
                        selectedTextColor = DensePrimary,
                        indicatorColor = DenseAccent,
                        unselectedIconColor = DenseTextSecondary,
                        unselectedTextColor = DenseTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_setup")
                )

                // CLI Navigation Tab
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Simulator CLI") },
                    label = { Text("CLI", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DensePrimary,
                        selectedTextColor = DensePrimary,
                        indicatorColor = DenseAccent,
                        unselectedIconColor = DenseTextSecondary,
                        unselectedTextColor = DenseTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_cli")
                )

                // Configs Navigation Tab
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Config Creators") },
                    label = { Text("Configs", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DensePrimary,
                        selectedTextColor = DensePrimary,
                        indicatorColor = DenseAccent,
                        unselectedIconColor = DenseTextSecondary,
                        unselectedTextColor = DenseTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_configs")
                )

                // Team Navigation Tab
                NavigationBarItem(
                    selected = selectedBottomTab == 3,
                    onClick = { selectedBottomTab = 3 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Team & Docs") },
                    label = { Text("Team", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DensePrimary,
                        selectedTextColor = DensePrimary,
                        indicatorColor = DenseAccent,
                        unselectedIconColor = DenseTextSecondary,
                        unselectedTextColor = DenseTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_team")
                )
            }
        },
        containerColor = DenseBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // High Density Header (Navbar Top Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(BorderStroke(1.dp, DenseBorder))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rounded Primary logo badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DensePrimary)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RX",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RexDevCyber X",
                            color = DenseTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = "v1.0.4 • Guide Control Center",
                            color = DenseTextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }

                // Green Online Pill Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DenseGreenBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DenseGreenText)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (localServerRunning) "PORT 3000 ONLINE" else "SYSTEM ONLINE",
                        color = DenseGreenText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Main Active Viewport content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedBottomTab) {
                    0 -> {
                        // SETUP TAB VIEW (Progress + Quick Action Grid + Checklist)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Progress Card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DenseAccent),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column {
                                                Text(
                                                    text = "PROJECT READINESS",
                                                    color = DensePrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Task Completion",
                                                    color = DenseTextPrimary,
                                                    fontWeight = FontWeight.Light,
                                                    fontSize = 22.sp
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${progressInfo.percentage.toInt()}%",
                                                    color = DensePrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 24.sp
                                                )
                                                Text(
                                                    text = "${progressInfo.completed} / ${progressInfo.total} Completed",
                                                    color = DenseTextSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Progress Track Bar
                                        val animatedPercent by animateFloatAsState(targetValue = progressInfo.percentage / 100f, label = "progress")
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.5f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(animatedPercent)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(DensePrimary)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Button Ribbon row inside Progress Card
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Cloud sync button
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isSyncingWithSheets = true
                                                        syncSuccessful = false
                                                        syncPhaseText = "กำลังติดต่อเซิร์ฟเวอร์ Google Cloud Sheets API..."
                                                        delay(500)
                                                        syncPhaseText = "ดึงแผนข้อมูลมัลติเพลเยอร์เรียบร้อย เข้าสู่โหมดสเปรดชีตทำงานสด..."
                                                        delay(500)
                                                        isSyncingWithSheets = false
                                                        isSheetsCollabOpen = true
                                                        viewModel.startCollabSession()
                                                        Toast.makeText(context, "เชื่อมต่อห้อง Google Sheets สำเร็จ!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.60f)),
                                                border = BorderStroke(1.dp, DensePrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("sheets_sync_button")
                                            ) {
                                                Text(
                                                    text = "📊 คลาวด์ซีทส์แชร์",
                                                    color = DensePrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Reset progress text action button
                                            Text(
                                                text = "รีเซ็ตสถานะ 🔄",
                                                color = QuickOrangeText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable { viewModel.resetAllProgress() }
                                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Action Grid (2x2)
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        // Card Env
                                        QuickActionCard(
                                            title = "Env",
                                            subtitle = "Node v20.12+",
                                            containerColor = QuickBlueBg,
                                            textColor = QuickBlueText,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            selectedWalkthroughPhase = 1
                                        }

                                        // Card Dev
                                        QuickActionCard(
                                            title = "Dev",
                                            subtitle = "Port 3000",
                                            containerColor = QuickPurpleBg,
                                            textColor = QuickPurpleText,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            selectedWalkthroughPhase = 2
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        // Card Translate (Crowdin)
                                        QuickActionCard(
                                            title = "Translate",
                                            subtitle = "Crowdin",
                                            containerColor = QuickAmberBg,
                                            textColor = QuickAmberText,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            selectedWalkthroughPhase = 3
                                        }

                                        // Card Deploy (Firebase Ready)
                                        QuickActionCard(
                                            title = "Deploy",
                                            subtitle = "Firebase Ready",
                                            containerColor = QuickOrangeBg,
                                            textColor = QuickOrangeText,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            selectedWalkthroughPhase = 4
                                        }
                                    }
                                }
                            }

                            // Detailed Task Section Header
                            item {
                                Text(
                                    text = "รายการเช็คความสมบูรณ์แบบละเอียด (Detailed Tasks Checklist)",
                                    color = DenseTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            // Checklist content categories and tasks
                            val groupedTasks = tasks.groupBy { it.category }
                            groupedTasks.forEach { (category, taskList) ->
                                item {
                                    Text(
                                        text = category,
                                        color = DensePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                items(taskList) { task ->
                                    TaskRowItem(
                                        task = task,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateTask(task.id, isChecked)
                                            if (isChecked) {
                                                viewModel.addTerminalLine("SUCCESS: Step #${task.id} set completed manually.", isSystem = true)
                                            } else {
                                                viewModel.addTerminalLine("INFO: Step #${task.id} toggled back to incomplete.", isSystem = true)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // FULL SIM-TERMINAL CLI TAB VIEW
                        FullTerminalSimulatorView(
                            terminalLogs = terminalLogs,
                            terminalInput = terminalInput,
                            isTerminalBusy = isTerminalBusy,
                            onCommandTriggered = { viewModel.triggerTerminalCommand(it) },
                            onInputUpdated = { viewModel.updateTerminalInput(it) }
                        )
                    }

                    2 -> {
                        // CONFIG BUILDERS TAB VIEW
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Crowdin card generator
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(BorderStroke(1.dp, DenseBorder), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚙️ ตัวสร้างไฟล์ crowdin.yml",
                                                color = DensePrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )

                                            Button(
                                                onClick = {
                                                    val crowdinConfig = makeCrowdinYml(crowdinId, crowdinToken, crowdinSource, crowdinTranslation)
                                                    clipboardManager.setText(AnnotatedString(crowdinConfig))
                                                    Toast.makeText(context, "คัดลอก crowdin.yml เรียบร้อย!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DensePrimary.copy(alpha = 0.12f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("📋 คัดลอก", color = DensePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "หากประมวลโปรเจ็กต์แปลภาษา จัดการผ่านไฟล์ YAML คลาสสิกคู่การแปล",
                                            color = DenseTextSecondary,
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = crowdinId,
                                            onValueChange = { viewModel.updateCrowdinProjectId(it) },
                                            label = { Text("Project ID", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = DenseTextPrimary, fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DensePrimary,
                                                unfocusedBorderColor = DenseBorder
                                            )
                                        )

                                        OutlinedTextField(
                                            value = crowdinToken,
                                            onValueChange = { viewModel.updateCrowdinApiToken(it) },
                                            label = { Text("API Token Variable", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = DenseTextPrimary, fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DensePrimary,
                                                unfocusedBorderColor = DenseBorder
                                            )
                                        )

                                        OutlinedTextField(
                                            value = crowdinSource,
                                            onValueChange = { viewModel.updateCrowdinSource(it) },
                                            label = { Text("Source locale JSON path", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = DenseTextPrimary, fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DensePrimary,
                                                unfocusedBorderColor = DenseBorder
                                            )
                                        )

                                        OutlinedTextField(
                                            value = crowdinTranslation,
                                            onValueChange = { viewModel.updateCrowdinTranslation(it) },
                                            label = { Text("Translation target pattern", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = DenseTextPrimary, fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DensePrimary,
                                                unfocusedBorderColor = DenseBorder
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ไฟล์ crowdin.yml พรีวิว:",
                                            color = DenseTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ConsoleSurface)
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = makeCrowdinYml(crowdinId, crowdinToken, crowdinSource, crowdinTranslation),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = ConsoleText,
                                                lineHeight = 16.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            // Firebase card builder
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(BorderStroke(1.dp, DenseBorder), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚙️ ตัวสร้างไฟล์ firebase.json",
                                                color = DensePrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )

                                            Button(
                                                onClick = {
                                                    val firebaseConfig = makeFirebaseJson(firebasePublicDir, firebaseSinglePageApp, firebaseIgnores)
                                                    clipboardManager.setText(AnnotatedString(firebaseConfig))
                                                    Toast.makeText(context, "คัดลอก firebase.json เรียบร้อย!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DensePrimary.copy(alpha = 0.12f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("📋 คัดลอก", color = DensePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ปรับตั้งค่าพารามิเตอร์ static ไฮสปีด CDN สำหรับ Firebase Hosting",
                                            color = DenseTextSecondary,
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = firebasePublicDir,
                                            onValueChange = { viewModel.updateFirebasePublicDir(it) },
                                            label = { Text("Public Directory Target", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = DenseTextPrimary, fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DensePrimary,
                                                unfocusedBorderColor = DenseBorder
                                            )
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Single Page App Rewrite", color = DenseTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text("เปลี่ยนทาง URL ตกหล่นทั้งหมดไป /index.html", color = DenseTextSecondary, fontSize = 10.sp)
                                            }
                                            Switch(
                                                checked = firebaseSinglePageApp,
                                                onCheckedChange = { viewModel.updateFirebaseSinglePageApp(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = DensePrimary,
                                                    checkedTrackColor = DensePrimary.copy(alpha = 0.4f)
                                                )
                                            )
                                        }

                                        OutlinedTextField(
                                            value = firebaseIgnores,
                                            onValueChange = { viewModel.updateFirebaseIgnores(it) },
                                            label = { Text("Ignored directories / files pattern", fontSize = 11.sp) },
                                            textStyle = TextStyle(fontSize = 12.sp, color = DenseTextPrimary, fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DensePrimary,
                                                unfocusedBorderColor = DenseBorder
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ไฟล์ firebase.json พรีวิว:",
                                            color = DenseTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ConsoleSurface)
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = makeFirebaseJson(firebasePublicDir, firebaseSinglePageApp, firebaseIgnores),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = ConsoleText,
                                                lineHeight = 16.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // TEAM & ABOUT DOCS VIEW
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Team Card info
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(BorderStroke(1.dp, DenseBorder), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "👥 Project Owner & Credits",
                                            color = DensePrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "ผู้ดูแลโครงการและระบบการตั้งค่า:\njokraxfluk@gmail.com",
                                            color = DenseTextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "RexDevCyber X • คู่มือการตั้งค่าและ deploy แบบโต้ตอบเต็มรูปแบบด้วย React และ Tailwind CSS ช่วยลดความผิดพลาดในการเตรียมความพร้อมระบบ มอบผลลัพธ์การทำงานที่มีความสม่ำเสมอและมีความเสถียรสูงสุดในระดับองค์กร",
                                            color = DenseTextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            // Commands Cookbook Card Reference
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(BorderStroke(1.dp, DenseBorder), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "📖 Commands Cookbook",
                                            color = DensePrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "คำสั่งทั่วไปในระดับสากลสำหรับนักพัฒนา:",
                                            color = DenseTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        val commandRef = listOf(
                                            "node -v" to "ตรวจหาเวอร์ชัน Node.js ปลั๊กอินในตัวเครื่อง",
                                            "npm install" to "ดึงและติดตั้งไฟล์โมดูลย่อยทั้งหมดของแพ็กเกจเว็ป",
                                            "npm run dev" to "เรียกเปิดเซิร์ฟเวอร์รันบน Local (พอร์ต 3000)",
                                            "firebase login" to "ประสานบัญชี Google Account ผ่านเว็บเอาเทนทิเคชัน",
                                            "firebase deploy" to "ส่งเนื้อหา CDN ข้อมูลเว็บสู่โดเมนจริงแบบเรียลไทม์"
                                        )

                                        commandRef.forEach { (cmdName, desc) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = cmdName,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = DensePrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(DenseAccent, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )

                                                Text(
                                                    text = desc,
                                                    color = DenseTextSecondary,
                                                    fontSize = 11.sp,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Expandable Walkthrough Details dialog descriptions
    if (selectedWalkthroughPhase != null) {
        val phase = selectedWalkthroughPhase!!
        val titleText = when (phase) {
            1 -> "1. กำหนดค่าสภาพแวดล้อม (Configure Environment)"
            2 -> "2. ตั้งค่าและเตรียมรันโปรเจ็กต์ (Setup & Run Project)"
            3 -> "3. เชื่อมโยงและแปลภาษา (Crowdin Localization)"
            else -> "4. จัดโปรเจ็กต์ขึ้นคลาวด์ (Deploy to Firebase)"
        }
        val detailContent = when (phase) {
            1 -> """
                การเขียนเว็บด้วยสแต็คสมัยใหม่จำเป็นต้องติดตั้ง Node.js
                
                ■ ดาวน์โหลดรุ่นเสถียร (LTS) v20.12 ขึ้นไป
                ■ ชุดติดตั้งจะผนวกตัวจัดการแพ็คเกจ npm โดยอัตโนมัติ
                
                ตรวจสอบการติดตั้งผ่านเทอร์มินัล:
                $ node -v
                $ npm -v
            """.trimIndent()
            2 -> """
                ขั้นตอนเริ่มต้นโปรแกรมบนเครื่องคุณเพื่อทำการพัฒนาเชิงรุก
                
                ■ รันคำสั่ง npm install เพื่อติดตั้งโมดูลย่อยทั้งหมด
                ■ เปิดเล่นเซิร์ฟเวอร์นักพัฒนาภายในเครื่องของคุณ:
                  $ npm run dev
                  
                เซิร์ฟเวอร์จะคอยสแตนด์บายตรวจดูโค้ดบนพอร์ต 3000 มอบระบบแล็บทดลองให้คุณแบบเรียลไทม์
            """.trimIndent()
            3 -> """
                เทคโนโลยี Crowdin จัดการแปลคลาวด์แยกส่วนภาษาอย่างเป็นอิสระ
                
                ■ ระบุกลุ่มภาษา เช่น อังกฤษ (en) - ไทย (th) ในหน้าเว็บไซต์ Crowdin
                ■ สร้างไฟล์ crowdin.yml ไว้ใต้โฟลเดอร์โครงการเพื่อควบคุมโครงร่างภาษา
                ■ เชื่อมโยงด้วย API Token ส่วนตัวจัดเก็บเป็นความลับสูงสุด
            """.trimIndent()
            else -> """
                อัปโหลดแจกจ่ายโปรเจกต์ของคุณสู่หน้าแชร์เว็บระดับสากลด้วย Firebase
                
                ■ ตรวจสอบโดยลงชื่อใช้งานด้วยเครื่องมือ CLI:
                  $ firebase login
                ■ ทำการคอมไพล์แปลงและรวบรวมหน้าเว็บเพื่อออฟติไมซ์:
                  $ npm run build
                ■ จัดส่ง static แพ็กเกจไคลเอนต์ขึ้น Hosting รวดเร็วพริบตา:
                  $ firebase deploy
            """.trimIndent()
        }
        val relativeCommand = when (phase) {
            1 -> "node -v"
            2 -> "npm run dev"
            3 -> "help"
            else -> "firebase deploy"
        }

        Dialog(onDismissRequest = { selectedWalkthroughPhase = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, DensePrimary, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = titleText,
                        color = DensePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = detailContent,
                        color = DenseTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { selectedWalkthroughPhase = null }
                        ) {
                            Text("เสร็จสิ้น", color = DenseTextSecondary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                selectedWalkthroughPhase = null
                                selectedBottomTab = 1 // Auto route user to the CLI Console Tab!
                                viewModel.triggerTerminalCommand(relativeCommand)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DensePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ลองรันใน CLI 🖥️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Google Sheets Sync Loading Modal
    if (isSyncingWithSheets) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, DenseBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = DensePrimary,
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Google Sheets Sync",
                        color = DensePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncPhaseText,
                        color = DenseTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Google Sheets Real-Time Collaboration Workspace
    if (isSheetsCollabOpen) {
        Dialog(
            onDismissRequest = {
                viewModel.stopCollabSession()
                isSheetsCollabOpen = false
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, DenseBorder), RoundedCornerShape(16.dp)),
                color = Color.White
            ) {
                SheetsCollaborationWorkspace(
                    viewModel = viewModel,
                    tasks = tasks,
                    collaborators = collaborators,
                    sheetsLogs = sheetsLogs,
                    localSelectedCell = localSelectedCell,
                    onClose = {
                        viewModel.stopCollabSession()
                        isSheetsCollabOpen = false
                    }
                )
            }
        }
    }
}

// Compact Quick Action grid menu item card helper
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(BorderStroke(1.dp, DenseBorder), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Rounded Icon shape backplate 
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                val iconSymbol = when (title) {
                    "Env" -> "💻"
                    "Dev" -> "⚡"
                    "Translate" -> "🌐"
                    else -> "🚀"
                }
                Text(text = iconSymbol, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = DenseTextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = (-0.2).sp
            )
            Text(
                text = subtitle,
                color = DenseTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

// Compact high density list check row representing task entity
@Composable
fun TaskRowItem(
    task: TaskEntity,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) Color(0xFFF1F5F9) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                1.dp,
                if (task.isCompleted) Color(0xFFCBD5E1) else DenseBorder,
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("task_check_${task.id}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = DensePrimary,
                    uncheckedColor = DenseTextSecondary,
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${task.id}. ${task.title}",
                    color = if (task.isCompleted) DensePrimary else DenseTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                Text(
                    text = task.description,
                    color = if (task.isCompleted) DenseTextSecondary.copy(alpha = 0.7f) else DenseTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// Beautiful Full Screen Simulated Console view
@Composable
fun FullTerminalSimulatorView(
    terminalLogs: List<TerminalLogLine>,
    terminalInput: String,
    isTerminalBusy: Boolean,
    onCommandTriggered: (String) -> Unit,
    onInputUpdated: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ConsoleBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // UI Window Gems
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFEF4444)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFF59E0B)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF10B981)))
                }

                Text(
                    text = "rx-dev@cyber-x: ~",
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "v1.0.4",
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }

            // Commands autocomplete quick chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val promptChips = listOf(
                    "node -v", "npm install", "npm run dev",
                    "firebase login", "firebase init", "npm run build", "firebase deploy"
                )
                promptChips.forEach { chipCommand ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                            .clickable(enabled = !isTerminalBusy) {
                                onCommandTriggered(chipCommand)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = chipCommand,
                            color = if (isTerminalBusy) Color(0xFF64748B) else Color(0xFF60A5FA),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // LazyColumn scroll view
            val terminalListState = rememberLazyListState()
            LaunchedEffect(terminalLogs.size) {
                if (terminalLogs.isNotEmpty()) {
                    terminalListState.animateScrollToItem(terminalLogs.size - 1)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ConsoleSurface)
                    .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = terminalListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(terminalLogs) { line ->
                        TerminalLineView(line)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Text execution helper action trigger
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "rx-dev@cyber-x:~ $ ",
                    color = Color(0xFF60A5FA),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                BasicTextFieldWithPlaceholder(
                    value = terminalInput,
                    onValueChange = onInputUpdated,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_text_input"),
                    placeholder = "ป้อนชุดคำสั่งที่นี่...",
                    enabled = !isTerminalBusy,
                    onSubmit = {
                        onCommandTriggered(terminalInput)
                    }
                )

                if (isTerminalBusy) {
                    CircularProgressIndicator(
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "รัน ↵",
                        color = ConsoleText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onCommandTriggered(terminalInput) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalLineView(line: TerminalLogLine) {
    val styleText = when {
        line.isPrompt -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)) {
                append("rx-dev@cyber-x:~ $ ")
            }
            append(line.text.removePrefix("rx-developer@cyber-x:~ $ ").removePrefix("rx-dev@cyber-x:~ $ "))
        }
        line.isSystem -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = ConsoleText, fontWeight = FontWeight.SemiBold)) {
                append("[SYSTEM] ")
            }
            append(line.text)
        }
        line.isError -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)) {
                append("[ERR] ")
            }
            append(line.text)
        }
        line.isLight -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.White)) {
                append(line.text)
            }
        }
        else -> buildAnnotatedString {
            append(line.text)
        }
    }

    Text(
        text = styleText,
        color = Color(0xFF94A3B8),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BasicTextFieldWithPlaceholder(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    enabled: Boolean,
    onSubmit: () -> Unit
) {
    Box(
        modifier = modifier.padding(horizontal = 6.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            textStyle = TextStyle(
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Config yaml builder helper
fun makeCrowdinYml(projectId: String, tokenVar: String, sourcePath: String, translationPattern: String): String {
    return """
# Crowdin CLI Configuration File
# Generated interactively via RexDevCyber X Setup Guide Assistant

project_id: "$projectId"
api_token_env: "$tokenVar"
base_path: "."

files:
  - source: "$sourcePath"
    translation: "$translationPattern"
    languages_mapping:
      two_letters_code:
        th: "th"
        en: "en"
""".trimIndent()
}

// Config json builder helper
fun makeFirebaseJson(publicDir: String, isSpa: Boolean, ignoreString: String): String {
    val ignoreList = ignoreString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val ignoresFormatted = ignoreList.joinToString(separator = ",\n      ") { "\"$it\"" }
    
    return """
{
  "hosting": {
    "public": "$publicDir",
    "ignore": [
      $ignoresFormatted
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}
""".trimIndent()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

// Global helper to parse hex string dynamically inside Compose
fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun SheetsCollaborationWorkspace(
    viewModel: MainViewModel,
    tasks: List<TaskEntity>,
    collaborators: List<Collaborator>,
    sheetsLogs: List<SheetsCollabLog>,
    localSelectedCell: Pair<Int, Int>?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // 1. Google Sheets Style Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF15803D)) // Google Sheets Forest Green!
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Spreadsheet Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "RexDevCyber_Checklist_Local_V1.xlsx",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF4ADE80))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "เสถียรภาพเรียลไทม์ • เชื่อมต่อผู้ใช้ 4 คน",
                            color = Color(0xFFDCFCE7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collaborator Avatar Bubbles in Toolbar
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-8).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Local user bubble
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF22C55E))
                            .border(1.5.dp, Color.White, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ME", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    
                    // Virtual collaborators
                    collaborators.forEach { collab ->
                        val initial = collab.name.take(2).uppercase()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(parseHexColor(collab.colorHex))
                                .border(1.5.dp, Color.White, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                // Exit Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ปิดแผ่นงาน",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // 2. Collaborative Active User Banner Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .border(BorderStroke(1.dp, DenseBorder))
                .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "⚡ ประสานงานสด: ",
                color = Color(0xFFEA580C),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
            val editingPeer = collaborators.find { it.isEditing }
            if (editingPeer != null) {
                Text(
                    text = "${editingPeer.name} กำลังพิมพ์สดที่แถว ${editingPeer.row + 1}...",
                    color = parseHexColor(editingPeer.colorHex),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    text = "สแตนด์บาย ทุกคนกำลังแก้ไขพร้อมกันและย้ายตำแหน่งเคอร์เซอร์",
                    color = DenseTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Helper states for formula bar
        val isCellEditable = { r: Int, c: Int -> c in 2..4 }
        val cellAddressName = { r: Int, c: Int ->
            val colLetter = ('A'.code + c).toChar()
            "$colLetter${r + 1}"
        }

        // 3. Sheets style Formula / Edit Input Bar (FX Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, DenseBorder))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "fx",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF15803D),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(DenseBorder)
                    .padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFDCFCE7))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = localSelectedCell?.let { (r, c) -> cellAddressName(r, c) } ?: "เลือกช่อง",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            var formulaTextState by remember { mutableStateOf("") }
            
            LaunchedEffect(localSelectedCell, tasks) {
                localSelectedCell?.let { (r, c) ->
                    val task = tasks.getOrNull(r)
                    if (task != null) {
                        formulaTextState = when (c) {
                            0 -> task.id.toString()
                            1 -> task.category
                            2 -> task.title
                            3 -> task.description
                            4 -> if (task.isCompleted) "COMPLETED" else "INCOMPLETE"
                            5 -> if (task.isCompleted) "System Verified" else "Awaiting Check"
                            else -> ""
                        }
                    }
                }
            }
            
            androidx.compose.foundation.text.BasicTextField(
                value = formulaTextState,
                onValueChange = {
                    formulaTextState = it
                },
                enabled = localSelectedCell != null && isCellEditable(localSelectedCell.first, localSelectedCell.second),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (localSelectedCell != null) DenseTextPrimary else Color.Gray
                ),
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (localSelectedCell != null && isCellEditable(localSelectedCell.first, localSelectedCell.second)) Color.White else Color(0xFFF1F5F9),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        if (localSelectedCell != null && isCellEditable(localSelectedCell.first, localSelectedCell.second)) Color(0xFF15803D) else DenseBorder,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    localSelectedCell?.let { (r, c) ->
                        viewModel.editLocalCell(r, c, formulaTextState)
                        Toast.makeText(context, "แก้ไขเนื้อหาช่องสำเร็จ!", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = localSelectedCell != null && isCellEditable(localSelectedCell.first, localSelectedCell.second),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("บันทึกช่อง 💾", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        // 4. Interactive Spreadsheet Scrollable Grid
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Header Titles Row
                Row(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9))
                        .border(BorderStroke(0.5.dp, DenseBorder))
                ) {
                    // Cell 0 Row index label
                    Box(modifier = Modifier.width(36.dp).height(28.dp).border(0.5.dp, DenseBorder), contentAlignment = Alignment.Center) {
                        Text("", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    val headers = listOf("A: ID", "B: Category", "C: Task Title", "D: Description", "E: Status", "F: Last Peer Activity")
                    val widths = listOf(40.dp, 100.dp, 160.dp, 240.dp, 100.dp, 120.dp)
                    
                    headers.forEachIndexed { idx, title ->
                        Box(
                            modifier = Modifier
                                .width(widths[idx])
                                .height(28.dp)
                                .border(0.5.dp, DenseBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DenseTextSecondary
                            )
                        }
                    }
                }
                
                // Rows Loop for the 9 Database tasks
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasks.size) { rowIndex ->
                        val task = tasks[rowIndex]
                        val rowNum = rowIndex + 1
                        
                        Row(
                            modifier = Modifier.background(
                                if (task.isCompleted) Color(0xFFF0FDF4) else Color.White
                            )
                        ) {
                            // Row Number label
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(44.dp)
                                    .background(Color(0xFFF8FAFC))
                                    .border(0.5.dp, DenseBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rowNum.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DenseTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            val widths = listOf(40.dp, 100.dp, 160.dp, 240.dp, 100.dp, 120.dp)
                            
                            // Let's render 6 columns per row
                            for (colIndex in 0..5) {
                                val valueText = when (colIndex) {
                                    0 -> task.id.toString()
                                    1 -> task.category
                                    2 -> task.title
                                    3 -> task.description
                                    4 -> if (task.isCompleted) "COMPLETED" else "INCOMPLETE"
                                    5 -> if (task.isCompleted) "System Verified" else "Awaiting Check"
                                    else -> ""
                                }
                                
                                val isSelected = localSelectedCell?.first == rowIndex && localSelectedCell.second == colIndex
                                val isCellCollabEditing = collaborators.any { it.row == rowIndex && it.col == colIndex && it.isEditing }
                                val cellCollab = collaborators.find { it.row == rowIndex && it.col == colIndex }
                                
                                // Determine border & styling logic for collaborative edits
                                val cellBackground = when {
                                    isCellCollabEditing -> parseHexColor(cellCollab?.colorHex ?: "#FFFFFF").copy(alpha = 0.15f)
                                    isSelected -> Color(0xFFF3E8FF)
                                    else -> Color.Transparent
                                }
                                
                                val cellBorderColor = when {
                                    isSelected -> Color(0xFF15803D) // Green indicator for local
                                    cellCollab != null -> parseHexColor(cellCollab.colorHex) // Peer indicator
                                    else -> DenseBorder
                                }
                                
                                val cellBorderWidth = if (isSelected || cellCollab != null) 2.dp else 0.5.dp
                                
                                Box(
                                    modifier = Modifier
                                        .width(widths[colIndex])
                                        .height(44.dp)
                                        .background(cellBackground)
                                        .border(cellBorderWidth, cellBorderColor)
                                        .clickable {
                                            viewModel.updateLocalSelection(rowIndex, colIndex)
                                        }
                                        .padding(4.dp),
                                    contentAlignment = if (colIndex == 0 || colIndex == 4) Alignment.Center else Alignment.CenterStart
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Floating Collaborator initial tag if cursor is here!
                                        if (cellCollab != null && !isSelected) {
                                            Text(
                                                text = cellCollab.name.take(2).uppercase() + (if (cellCollab.isEditing) " ✍" else ""),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .background(parseHexColor(cellCollab.colorHex), RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 2.dp)
                                            )
                                        }
                                        
                                        // Content text
                                        Text(
                                            text = valueText,
                                            fontSize = 9.sp,
                                            color = when {
                                                colIndex == 4 && task.isCompleted -> Color(0xFF166534)
                                                colIndex == 4 -> Color(0xFF9A3412)
                                                else -> DenseTextPrimary
                                            },
                                            fontFamily = if (colIndex == 0) FontFamily.Monospace else FontFamily.SansSerif,
                                            fontWeight = if (colIndex == 4) FontWeight.Bold else FontWeight.Normal,
                                            lineHeight = 11.sp,
                                            textAlign = if (colIndex == 0 || colIndex == 4) TextAlign.Center else TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth().weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 5. Activity Event Logs Screen Panel & Bottom Control bar
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F172A)) // dark log background
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF38BDF8)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Real-Time Collaboration Event Stream Logs",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Burst edit trigger button
                Text(
                    text = "ป้อนข้อมูลรัวๆ 💥",
                    color = Color(0xFFFBBF24),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                viewModel.addCollabLog("คุณ สั่งการประมวลผลแก้ไขแบบกลุ่มพร้อมผู้ใช้รอบโครงสร้าง...")
                                delay(300)
                                viewModel.editLocalCell(2, 4, "COMPLETED")
                                viewModel.addCollabLog("Somchai L. สลับ แถว 3 -> COMPLETED")
                                viewModel.editLocalCell(3, 4, "COMPLETED")
                                viewModel.addCollabLog("Cyber Admin สลับ แถว 4 -> COMPLETED")
                                Toast.makeText(context, "มัลติเพลเยอร์ระเบิดแก้ไขขั้นตอนเรียบร้อย!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            // Log output stream scrolling
            val logsScrollState = rememberLazyListState()
            LaunchedEffect(sheetsLogs.size) {
                if (sheetsLogs.isNotEmpty()) {
                    logsScrollState.animateScrollToItem(sheetsLogs.size - 1)
                }
            }
            
            LazyColumn(
                state = logsScrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                items(sheetsLogs) { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "[${log.timestamp}]",
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = log.message,
                            color = when {
                                log.message.contains("คุณ") -> Color(0xFF34D399) // green-400 for you
                                log.message.contains("ระบบ") -> Color(0xFFF43F5E) // red-500
                                log.message.contains("Somchai") -> Color(0xFF60A5FA)
                                log.message.contains("Cyber Admin") -> Color(0xFFF87171)
                                else -> Color(0xFFE2E8F0)
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Quick instructions
            Text(
                text = "* เกร็ดแนะนำ: คลิ๊กที่กล่องตารางข้อความใดเพื่อเลือก และใช้แถบสูตรด้านบน (fx) เพื่อทำการพิมพ์เปลี่ยนแปลงข้อมูล",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
