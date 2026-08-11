package com.example.ui.screens.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.ExecutionPlanInterviewEntity
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import com.example.data.ChatMessageEntity
import com.example.data.ChatRepository
import com.example.data.ChatSessionEntity
import com.example.data.UserRepository
import com.example.ui.components.FoundryCard
import com.example.ui.components.FoundrySymbol
import com.example.util.LocationHelper
import com.example.util.TtsManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val activeUser by userRepository.activeUserFlow.collectAsState(initial = null)
    val userName = activeUser?.displayName.orEmpty().ifBlank { "User" }
    val userId = activeUser?.userId ?: ""
    val userPersonality = activeUser?.personalityPreference ?: "BALANCED"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Real Chat Sessions Flow with search support
    var searchQuery by remember { mutableStateOf("") }
    val sessions by chatRepository.searchSessionsForUserFlow(userId, searchQuery).collectAsState(initial = emptyList())

    // Active Chat Session ID
    var activeSessionId by remember { mutableStateOf<String?>(null) }

    // Sync conversations from Firestore for authenticated user
    LaunchedEffect(userId) {
        activeSessionId = null
        if (userId.isNotBlank()) {
            chatRepository.syncFromFirestore(userId)
        }
    }

    // Reset active session if the deleted session was currently active
    LaunchedEffect(sessions) {
        if (activeSessionId != null && sessions.none { it.sessionId == activeSessionId }) {
            activeSessionId = null
        }
    }

    // Messages Flow & Interview State Flow for current active session
    val messages by chatRepository.getMessagesForSessionFlow(activeSessionId).collectAsState(initial = emptyList())
    val interview by chatRepository.getInterviewFlow(activeSessionId).collectAsState(initial = null)

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var currentGenerationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // AI Capability States
    var isSearchEnabled by remember { mutableStateOf(false) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    var locationContext by remember { mutableStateOf<String?>(null) }
    var isDeepReasoningEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Voice Output TTS Manager
    val ttsManager = remember { TtsManager(context) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    // Speech-To-Text Manager for live streaming speech dictation
    val speechToTextManager = remember { com.example.util.SpeechToTextManager(context) }
    val isSttListening by speechToTextManager.isListening.collectAsState()
    val isListeningActive = isListening || isSttListening

    DisposableEffect(Unit) {
        ttsManager.onStateChanged = { isSpeaking ->
            if (!isSpeaking) {
                speakingMessageId = null
            }
        }
        onDispose {
            ttsManager.shutdown()
            speechToTextManager.destroy()
        }
    }

    // Speech Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spokenText else "$inputText $spokenText"
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(context, "Speech transcription was interrupted or unavailable.", Toast.LENGTH_SHORT).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate your idea...")
            }
            try {
                isListening = true
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                isListening = false
                Toast.makeText(context, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            isListening = false
            Toast.makeText(context, "Microphone permission is required for Speech-to-Text", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                locationContext = LocationHelper.getCurrentLocationDescription(context) ?: "Location enabled"
                isLocationEnabled = true
                Toast.makeText(context, "Location context enabled: $locationContext", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Location permission denied. Text conversation remains fully functional.", Toast.LENGTH_SHORT).show()
            isLocationEnabled = false
        }
    }

    val startDictation = {
        ttsManager.stop()
        speakingMessageId = null
        if (isListeningActive) {
            speechToTextManager.stopListening()
            isListening = false
        } else if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val initialText = inputText
            speechToTextManager.onPartialResult = { partial ->
                inputText = if (initialText.isBlank()) partial else "$initialText $partial"
            }
            speechToTextManager.onFinalResult = { final ->
                inputText = if (initialText.isBlank()) final else "$initialText $final"
                isListening = false
            }
            speechToTextManager.onError = { _ ->
                isListening = false
                // Fallback to RecognizerIntent activity launcher
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate your message...")
                }
                try {
                    isListening = true
                    speechLauncher.launch(intent)
                } catch (e: Exception) {
                    isListening = false
                    Toast.makeText(context, "Microphone dictation unavailable.", Toast.LENGTH_SHORT).show()
                }
            }
            speechToTextManager.startListening()
        } else {
            isListening = false
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleStopGeneration: () -> Unit = {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        isGeneratingAi = false
        isSending = false
        chatError = "Generation stopped by user."
    }

    val handleSendMessage: (String) -> Unit = { text ->
        if (text.isNotBlank() && !isSending && !isGeneratingAi) {
            ttsManager.stop()
            speakingMessageId = null
            val textToSend = text.trim()
            inputText = ""
            chatError = null
            isSending = true
            currentGenerationJob = coroutineScope.launch {
                try {
                    var targetSid = activeSessionId
                    if (targetSid == null) {
                        val created = chatRepository.createSession(userId, "New Chat")
                        targetSid = created.sessionId
                        activeSessionId = targetSid
                    }
                    chatRepository.saveUserMessage(targetSid, textToSend)
                    isSending = false
                    isGeneratingAi = true

                    val currentUserName = activeUser?.displayName.takeIf { !it.isNullOrBlank() } ?: activeUser?.email
                    val aiResult = chatRepository.generateAndSaveAiResponse(
                        sessionId = targetSid,
                        personality = userPersonality,
                        userName = currentUserName,
                        enableSearchGrounding = isSearchEnabled,
                        locationContext = if (isLocationEnabled) locationContext else null,
                        enableDeepReasoning = isDeepReasoningEnabled
                    )
                    if (aiResult.isFailure) {
                        chatError = aiResult.exceptionOrNull()?.message ?: "Failed to generate AI response."
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    chatError = "Generation stopped by user."
                    throw e
                } catch (e: Exception) {
                    if (isSending) {
                        inputText = textToSend
                    }
                    chatError = e.localizedMessage ?: "Failed to send message."
                } finally {
                    isSending = false
                    isGeneratingAi = false
                    currentGenerationJob = null
                }
            }
        }
    }

    val handleEditMessage: (String, String) -> Unit = { messageId, newText ->
        if (newText.isNotBlank() && !isSending && !isGeneratingAi) {
            val targetSid = activeSessionId
            if (targetSid != null) {
                ttsManager.stop()
                speakingMessageId = null
                chatError = null
                isGeneratingAi = true
                currentGenerationJob = coroutineScope.launch {
                    try {
                        val currentUserName = activeUser?.displayName.takeIf { !it.isNullOrBlank() } ?: activeUser?.email
                        val aiResult = chatRepository.editUserMessageAndRegenerate(
                            sessionId = targetSid,
                            messageId = messageId,
                            newContent = newText,
                            personality = userPersonality,
                            userName = currentUserName,
                            enableSearchGrounding = isSearchEnabled,
                            locationContext = if (isLocationEnabled) locationContext else null,
                            enableDeepReasoning = isDeepReasoningEnabled
                        )
                        if (aiResult.isFailure) {
                            chatError = aiResult.exceptionOrNull()?.message ?: "Failed to regenerate AI response."
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        chatError = "Generation stopped by user."
                        throw e
                    } catch (e: Exception) {
                        chatError = e.localizedMessage ?: "Failed to edit message."
                    } finally {
                        isGeneratingAi = false
                        currentGenerationJob = null
                    }
                }
            }
        }
    }

    val handleRetryAi: () -> Unit = {
        val targetSid = activeSessionId
        if (targetSid != null && !isSending && !isGeneratingAi) {
            chatError = null
            isGeneratingAi = true
            currentGenerationJob = coroutineScope.launch {
                try {
                    val currentUserName = activeUser?.displayName.takeIf { !it.isNullOrBlank() } ?: activeUser?.email
                    val aiResult = chatRepository.generateAndSaveAiResponse(
                        sessionId = targetSid,
                        personality = userPersonality,
                        userName = currentUserName,
                        enableSearchGrounding = isSearchEnabled,
                        locationContext = if (isLocationEnabled) locationContext else null,
                        enableDeepReasoning = isDeepReasoningEnabled
                    )
                    if (aiResult.isFailure) {
                        chatError = aiResult.exceptionOrNull()?.message ?: "Failed to generate AI response."
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    chatError = "Generation stopped by user."
                    throw e
                } catch (e: Exception) {
                    chatError = e.localizedMessage ?: "Retry failed."
                } finally {
                    isGeneratingAi = false
                    currentGenerationJob = null
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                SidebarContent(
                    userName = userName,
                    userEmail = activeUser?.email ?: "",
                    sessions = sessions,
                    activeSessionId = activeSessionId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSelectSession = { session ->
                        activeSessionId = session.sessionId
                        chatError = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        activeSessionId = null
                        chatError = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteSession = { sessionId ->
                        coroutineScope.launch {
                            chatRepository.deleteSession(userId, sessionId)
                            if (activeSessionId == sessionId) {
                                activeSessionId = sessions.firstOrNull { it.sessionId != sessionId }?.sessionId
                                chatError = null
                            }
                        }
                    },
                    onTogglePinSession = { sessionId ->
                        coroutineScope.launch {
                            chatRepository.togglePinSession(userId, sessionId)
                        }
                    },
                    onRenameSession = { sessionId, newTitle ->
                        coroutineScope.launch {
                            chatRepository.updateSessionTitle(userId, sessionId, newTitle)
                        }
                    },
                    onNavigateToSettings = {
                        coroutineScope.launch {
                            drawerState.close()
                            onNavigateToSettings()
                        }
                    },
                    onLogout = {
                        coroutineScope.launch {
                            drawerState.close()
                            userRepository.authSessionManager.logout()
                            onLogout()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FoundrySymbol(size = 28.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "FOUNDRY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Sidebar Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                activeSessionId = null
                                chatError = null
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val listState = rememberLazyListState()

                val isAtBottom by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val totalItemsNumber = layoutInfo.totalItemsCount
                        if (totalItemsNumber == 0) true
                        else {
                            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleItemIndex >= totalItemsNumber - 2
                        }
                    }
                }

                // Scroll to bottom when new messages, loading state, or error state arrive
                LaunchedEffect(messages.size, isGeneratingAi, chatError) {
                    val totalItems = messages.size + (if (isGeneratingAi) 1 else 0) + (if (chatError != null) 1 else 0)
                    if (totalItems > 0) {
                        listState.animateScrollToItem(totalItems - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty() && !isGeneratingAi && chatError == null) {
                        // Display clean greeting header when starting fresh
                        NewChatGreetingHeader(
                            userName = userName,
                            onPromptSelected = { prompt ->
                                handleSendMessage(prompt)
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            // Continuation Header Banner / Interview Banner
                            val currentInterview = interview
                            if (currentInterview != null && currentInterview.status != "NOT_STARTED") {
                                InterviewProgressCard(
                                    interview = currentInterview,
                                    onPauseClick = { handleSendMessage("pause interview") },
                                    onResumeClick = { handleSendMessage("resume interview") },
                                    onResetClick = {
                                        activeSessionId?.let { sid ->
                                            coroutineScope.launch {
                                                chatRepository.resetInterview(sid, userId)
                                                handleSendMessage("create personalized execution plan")
                                            }
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FoundrySymbol(size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Hi $userName, ready to continue your idea",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(messages, key = { it.messageId }) { msg ->
                                        Column {
                                            AnimatedVisibility(
                                                visible = true,
                                                enter = fadeIn() + slideInVertically { it / 2 }
                                            ) {
                                                ChatMessageBubble(
                                                    message = msg,
                                                    speakingMessageId = speakingMessageId,
                                                    onSpeakingToggle = { msgId, text ->
                                                        if (speakingMessageId == msgId) {
                                                            ttsManager.stop()
                                                            speakingMessageId = null
                                                        } else {
                                                            speakingMessageId = msgId
                                                            ttsManager.speak(text)
                                                        }
                                                    },
                                                    onEditMessage = { messageId, newText ->
                                                        handleEditMessage(messageId, newText)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (isGeneratingAi) {
                                        item(key = "ai_generating_indicator") {
                                            AiThinkingIndicator(
                                                isSearchEnabled = isSearchEnabled,
                                                isLocationEnabled = isLocationEnabled,
                                                isDeepReasoningEnabled = isDeepReasoningEnabled
                                            )
                                        }
                                    }

                                    if (chatError != null) {
                                        item(key = "ai_error_card") {
                                            FoundryCard(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "Error",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = chatError ?: "An error occurred.",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        Button(
                                                            onClick = { handleRetryAi() },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                            ),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Text("Retry AI Response", style = MaterialTheme.typography.labelMedium)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!isAtBottom && messages.isNotEmpty()) {
                                    FloatingActionButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val totalItems = messages.size + (if (isGeneratingAi) 1 else 0)
                                                if (totalItems > 0) {
                                                    listState.animateScrollToItem(totalItems - 1)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp)
                                            .size(40.dp),
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Scroll to bottom",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Capabilities Bar & Input Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // AI Capability Toggles Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 840.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isSearchEnabled,
                            onClick = { isSearchEnabled = !isSearchEnabled },
                            label = { Text("Google Search", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )

                        FilterChip(
                            selected = isLocationEnabled,
                            onClick = {
                                if (!isLocationEnabled) {
                                    if (LocationHelper.hasLocationPermission(context)) {
                                        coroutineScope.launch {
                                            locationContext = LocationHelper.getCurrentLocationDescription(context) ?: "Location active"
                                            isLocationEnabled = true
                                            Toast.makeText(context, "Location context active: $locationContext", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                    }
                                } else {
                                    isLocationEnabled = false
                                }
                            },
                            label = {
                                Text(
                                    text = if (isLocationEnabled && !locationContext.isNullOrBlank()) "Location: $locationContext" else "Maps / Location",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )

                        FilterChip(
                            selected = isDeepReasoningEnabled,
                            onClick = { isDeepReasoningEnabled = !isDeepReasoningEnabled },
                            label = { Text("Deep Reasoning", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    // Live Dictation Listening Banner
                    AnimatedVisibility(
                        visible = isListeningActive,
                        enter = fadeIn() + slideInVertically(),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 840.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Listening... Speak now to dictate",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = { startDictation() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Stop",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 840.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Type or dictate your idea...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = false,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    handleSendMessage(inputText)
                                }
                            ),
                            enabled = !isSending && !isGeneratingAi,
                            trailingIcon = {
                                IconButton(
                                    onClick = { startDictation() },
                                    enabled = !isSending && !isGeneratingAi,
                                    modifier = Modifier.testTag("mic_button")
                                ) {
                                    Icon(
                                        imageVector = if (isListeningActive) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = "Dictate idea with Speech to Text",
                                        tint = if (isListeningActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isGeneratingAi) {
                            IconButton(
                                onClick = { handleStopGeneration() },
                                modifier = Modifier.testTag("stop_generation_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop Generation",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    handleSendMessage(inputText)
                                },
                                enabled = inputText.isNotBlank() && !isSending,
                                modifier = Modifier.testTag("send_button")
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Message",
                                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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

@Composable
fun NewChatGreetingHeader(
    userName: String,
    onPromptSelected: (String) -> Unit
) {
    val altPrompts = remember(userName) {
        listOf(
            "Hi $userName, what shall we build today?",
            "Hi $userName, let's bring your ideas to life",
            "Hi $userName, ready to explore new concepts?"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FoundrySymbol(size = 64.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Hi, $userName, ready to start",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Type a prompt below or pick a sentence to begin your chat:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            altPrompts.forEach { prompt ->
                FoundryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPromptSelected(prompt) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiThinkingIndicator(
    isSearchEnabled: Boolean = false,
    isLocationEnabled: Boolean = false,
    isDeepReasoningEnabled: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val capabilityText = remember(isSearchEnabled, isLocationEnabled, isDeepReasoningEnabled) {
        val active = mutableListOf<String>()
        if (isSearchEnabled) active.add("Google Search")
        if (isLocationEnabled) active.add("Maps / Location")
        if (isDeepReasoningEnabled) active.add("Deep Reasoning")
        if (active.isNotEmpty()) "Foundry AI is analyzing with ${active.joinToString(", ")}..."
        else "Foundry AI is thinking..."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha),
            contentAlignment = Alignment.Center
        ) {
            FoundrySymbol(size = 18.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = capabilityText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )
    }
}

sealed class MarkdownBlock {
    data class TextBlock(val content: String) : MarkdownBlock()
    data class CodeBlock(val code: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val codeRegex = Regex("```(?:[a-zA-Z]*\\n)?([\\s\\S]*?)```")
    var lastIndex = 0

    for (match in codeRegex.findAll(text)) {
        if (match.range.first > lastIndex) {
            val textChunk = text.substring(lastIndex, match.range.first)
            if (textChunk.isNotBlank()) {
                blocks.add(MarkdownBlock.TextBlock(textChunk.trim()))
            }
        }
        val codeChunk = match.groupValues.getOrNull(1) ?: match.value
        blocks.add(MarkdownBlock.CodeBlock(codeChunk.trim()))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex)
        if (remaining.isNotBlank()) {
            blocks.add(MarkdownBlock.TextBlock(remaining.trim()))
        }
    }

    if (blocks.isEmpty()) {
        blocks.add(MarkdownBlock.TextBlock(text))
    }

    return blocks
}

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        var lastIndex = 0
        for (match in boldRegex.findAll(text)) {
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun FormattedText(
    text: String,
    textColor: Color,
    isUser: Boolean
) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val parts = remember(text) { parseMarkdownBlocks(text) }
            for (part in parts) {
                when (part) {
                    is MarkdownBlock.CodeBlock -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(12.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = part.code,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is MarkdownBlock.TextBlock -> {
                        val annotated = remember(part.content) { parseInlineMarkdown(part.content) }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    speakingMessageId: String? = null,
    onSpeakingToggle: (String, String) -> Unit = { _, _ -> },
    onEditMessage: (String, String) -> Unit = { _, _ -> }
) {
    val isUser = message.sender == "USER"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { dateFormat.format(Date(message.timestamp)) }
    val isSpeaking = speakingMessageId == message.messageId

    var isEditing by remember { mutableStateOf(false) }
    var editedContent by remember(message.content) { mutableStateOf(message.content) }

    val isExecutionPlan = remember(message.content) {
        !isUser && (
            message.content.contains("EXECUTION OBJECTIVE", ignoreCase = true) ||
            message.content.contains("PHASED EXECUTION PLAN", ignoreCase = true) ||
            message.content.contains("12. NEXT ACTION", ignoreCase = true) ||
            message.content.contains("Personalized Execution Plan", ignoreCase = true)
        )
    }
    var isPlanExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.widthIn(max = 680.dp)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    FoundrySymbol(size = 18.dp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    if (isEditing && isUser) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Edit message:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = editedContent,
                                onValueChange = { editedContent = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_message_input"),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        isEditing = false
                                        editedContent = message.content
                                    }
                                ) {
                                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (editedContent.isNotBlank()) {
                                            isEditing = false
                                            onEditMessage(message.messageId, editedContent)
                                        }
                                    },
                                    modifier = Modifier.testTag("save_and_regenerate_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save & Regenerate", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    } else if (isExecutionPlan) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Execution Plan",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = { isPlanExpanded = !isPlanExpanded },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isPlanExpanded) "Collapse" else "Expand",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = if (isPlanExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isPlanExpanded) {
                            FormattedText(
                                text = message.content,
                                textColor = textColor,
                                isUser = isUser
                            )
                        } else {
                            val previewText = remember(message.content) {
                                val firstLine = message.content.lines().firstOrNull { it.isNotBlank() } ?: "Personalized Execution Plan"
                                if (firstLine.length > 80) firstLine.take(80) + "..." else firstLine
                            }
                            Text(
                                text = "$previewText (Collapsed)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        }
                    } else {
                        FormattedText(
                            text = message.content,
                            textColor = textColor,
                            isUser = isUser
                        )
                    }

                    if (isUser && !isEditing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("edit_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit message and regenerate AI response",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSpeakingToggle(message.messageId, message.content) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop Voice Playback" else "Play AI Response Aloud",
                                    tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
fun SidebarContent(
    userName: String,
    userEmail: String,
    sessions: List<ChatSessionEntity>,
    activeSessionId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectSession: (ChatSessionEntity) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onTogglePinSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var sessionToRename by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    if (sessionToRename != null) {
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Chat Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val session = sessionToRename
                        if (session != null && renameText.isNotBlank()) {
                            onRenameSession(session.sessionId, renameText.trim())
                        }
                        sessionToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${sessionToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToDelete?.sessionId?.let { onDeleteSession(it) }
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        // App Identity Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
        ) {
            FoundrySymbol(size = 32.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "FOUNDRY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Forge Your Ideas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by keyword or project...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("conversation_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // New Chat Button
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "New Chat", fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

        val pinnedSessions = remember(sessions) { sessions.filter { it.isPinned } }
        val unpinnedSessions = remember(sessions) { sessions.filter { !it.isPinned } }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (pinnedSessions.isNotEmpty()) {
                Text(
                    text = "Pinned",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(0.4f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pinnedSessions, key = { "pinned_${it.sessionId}" }) { session ->
                        SidebarSessionItem(
                            session = session,
                            isActive = session.sessionId == activeSessionId,
                            onSelect = { onSelectSession(session) },
                            onTogglePin = { onTogglePinSession(session.sessionId) },
                            onRename = {
                                sessionToRename = session
                                renameText = session.title
                            },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Chat History",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (unpinnedSessions.isEmpty() && pinnedSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching conversations." else "No chat history yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(unpinnedSessions, key = { it.sessionId }) { session ->
                        SidebarSessionItem(
                            session = session,
                            isActive = session.sessionId == activeSessionId,
                            onSelect = { onSelectSession(session) },
                            onTogglePin = { onTogglePinSession(session.sessionId) },
                            onRename = {
                                sessionToRename = session
                                renameText = session.title
                            },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Bottom User / Settings Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSettings() }
                    .padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SidebarSessionItem(
    session: ChatSessionEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
    val textColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = session.title.ifBlank { "Untitled Chat" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = if (session.isPinned) "Unpin Chat" else "Pin Chat",
                tint = if (session.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(
            onClick = onRename,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename Chat",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Chat",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InterviewProgressCard(
    interview: ExecutionPlanInterviewEntity,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val currentIdx = interview.currentQuestionIndex.coerceIn(0, 9)
    val progress = when (interview.status) {
        "COMPLETED" -> 1.0f
        "IN_PROGRESS", "PAUSED" -> (currentIdx + 1) / 10f
        else -> 0f
    }
    var isPlanExpanded by remember { mutableStateOf(false) }

    FoundryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        containerColor = when (interview.status) {
            "IN_PROGRESS" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            "COMPLETED" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = when (interview.status) {
                            "COMPLETED" -> Icons.Default.CheckCircle
                            "IN_PROGRESS" -> Icons.Default.Assignment
                            else -> Icons.Default.PauseCircle
                        },
                        contentDescription = null,
                        tint = when (interview.status) {
                            "COMPLETED" -> MaterialTheme.colorScheme.tertiary
                            "IN_PROGRESS" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (interview.status) {
                            "COMPLETED" -> "Execution Plan Personalization Active"
                            "IN_PROGRESS" -> "Execution Plan Interview: Question ${currentIdx + 1} of 10"
                            "PAUSED" -> "Interview Paused (${currentIdx} of 10 saved)"
                            else -> "Execution Plan Interview"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (interview.status == "IN_PROGRESS") {
                        TextButton(onClick = onPauseClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Pause", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (interview.status == "PAUSED") {
                        Button(
                            onClick = onResumeClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Resume", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (interview.status == "COMPLETED") {
                        if (interview.generatedPlan.isNotBlank()) {
                            TextButton(
                                onClick = { isPlanExpanded = !isPlanExpanded },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(if (isPlanExpanded) "Hide Plan" else "View Plan", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = if (isPlanExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        TextButton(onClick = onResetClick, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text("Restart Plan", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (interview.status == "IN_PROGRESS" || interview.status == "PAUSED") {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (interview.status == "COMPLETED" && isPlanExpanded && interview.generatedPlan.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FormattedText(
                        text = interview.generatedPlan,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        isUser = false
                    )
                }
            }
        }
    }
}
