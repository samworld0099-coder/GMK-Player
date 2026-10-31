package com.example.feedback

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.donation.DonationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onBack()
    }

    val feedbackTypes = listOf("Suggestion", "Bug Report", "Feature Request", "UI/Design", "Performance", "Other")
    val appSections = listOf("File Manager", "Video Player", "Web", "Send & Receive", "Private Files", "Settings", "Other")

    var selectedType by remember { mutableStateOf("Suggestion") }
    var selectedSection by remember { mutableStateOf("Video Player") }
    var rating by remember { mutableIntStateOf(0) } // 0 means unrated
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    fun submitFeedback() {
        val trimmedMsg = message.trim()
        if (trimmedMsg.isBlank()) {
            errorMessage = "Please enter your feedback message."
            return
        }

        errorMessage = null
        isSubmitting = true

        scope.launch {
            // Simulate brief processing and save locally
            delay(400)
            val feedbackItem = FeedbackItem(
                id = "fb_${System.currentTimeMillis()}",
                type = selectedType,
                rating = rating,
                message = trimmedMsg,
                section = selectedSection,
                email = email.trim(),
                timestamp = System.currentTimeMillis(),
                appVersion = "1.0"
            )

            FeedbackManager.saveFeedback(context, feedbackItem)
            isSubmitting = false
            submissionSuccess = true
            message = ""
            snackbarHostState.showSnackbar("Thank you! Your feedback has been saved successfully.")
        }
    }

    fun openEmailClient() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${DonationManager.SUPPORT_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, "GMK Media Player - $selectedType ($selectedSection)")
            val body = "Category: $selectedType\nSection: $selectedSection\nRating: ${if (rating > 0) "$rating/5" else "N/A"}\n\nMessage:\n${message.ifBlank { "(Enter your feedback here)" }}\n\nApp Version: 1.0"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email"))
        } catch (_: Exception) {
            Toast.makeText(context, "No email client installed", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Feedback & Suggestions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("feedback_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("view_feedback_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Submitted Feedback History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("feedback_screen_container")
        ) {
            // Header Description Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your opinion matters!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Help us improve GMK Media Player. Tell us what you like, what is not working correctly, which features you want next, or what you think we should change. Your honest feedback helps us build a better app for everyone.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Feedback Type Selection
            Text("Feedback Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                feedbackTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Concerned Section Selection
            Text("App Section Concerned (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                appSections.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rating Stars (Optional)
            Text("Rating (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { rating = if (rating == star) 0 else star },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star Stars",
                            tint = if (star <= rating) Color(0xFFFBC02D) else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (rating > 0) {
                    Text("$rating / 5", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFBC02D))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feedback Message (Required)
            Text("Feedback Message *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    if (errorMessage != null) errorMessage = null
                },
                placeholder = { Text("Describe what is working well, bugs you found, or suggestions you have...", fontSize = 13.sp) },
                isError = errorMessage != null,
                minLines = 4,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_message_input")
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Email
            Text("Contact Email (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Your email address (only if you wish to receive a reply)", fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_email_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Storage Notice
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submissions are recorded locally on your device. You can also send feedback directly to the developer via email.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Button(
                onClick = { submitFeedback() },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_feedback_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Feedback", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Email directly button
            OutlinedButton(
                onClick = { openEmailClient() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_feedback_email_button")
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send via Email App (${DonationManager.SUPPORT_EMAIL})")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog showing past submitted feedback on this device
    if (showHistoryDialog) {
        val historyList = remember { FeedbackManager.getSavedFeedback(context) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Saved Feedback (${historyList.size})", fontWeight = FontWeight.Bold) },
            text = {
                if (historyList.isEmpty()) {
                    Text("No feedback recorded on this device yet.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList.size) { idx ->
                            val item = historyList[idx]
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.type, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.timestamp)), fontSize = 10.sp, color = Color.Gray)
                                    }
                                    if (item.section.isNotBlank()) {
                                        Text("Section: ${item.section}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.message, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHistoryDialog = false }) { Text("Close") }
            }
        )
    }
}
