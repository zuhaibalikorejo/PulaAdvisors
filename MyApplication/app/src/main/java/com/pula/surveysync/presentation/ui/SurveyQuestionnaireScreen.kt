package com.pula.surveysync.presentation.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.pula.surveysync.data.local.entity.AnswerValue
import com.pula.surveysync.data.local.entity.FarmerEntity
import com.pula.surveysync.presentation.viewmodel.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyQuestionnaireScreen(
    farmer: FarmerEntity,
    onBack: () -> Unit,
    onSurveyCompleted: () -> Unit,
    viewModel: SurveyViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val numberOfFarms by viewModel.numberOfFarms.collectAsState()
    val context = LocalContext.current

    // Reset survey state when screen is opened for a new farmer
    LaunchedEffect(farmer.id) {
        viewModel.resetSurvey()
    }

    // Handle validation errors with Toast
    LaunchedEffect(uiState) {
        if (uiState is SurveyUiState.ValidationError) {
            val errors = (uiState as SurveyUiState.ValidationError).missingFields
            val message = if (errors.size == 1) {
                "Missing: ${errors.first()}"
            } else {
                "Please fill ${errors.size} required fields:\n${errors.take(3).joinToString("\n")}"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearValidationError()
        }
    }

    // Handle survey completion
    LaunchedEffect(uiState) {
        if (uiState is SurveyUiState.Success) {
            onSurveyCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Survey: ${farmer.fullName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.submitSurvey(farmer.id)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is SurveyUiState.Saving
                    ) {
                        if (uiState is SurveyUiState.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (uiState is SurveyUiState.Saving) "Saving..." else "Submit Survey")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show error if any
            if (uiState is SurveyUiState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uiState as SurveyUiState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Farmer info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = farmer.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (farmer.location.isNotEmpty()) {
                        Text(
                            text = farmer.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Questions
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Non-repeating questions first
                viewModel.questions.filter { !it.isRepeating }.forEach { question ->
                    item(key = question.id) {
                        QuestionCard(
                            question = question,
                            answer = answers[question.id],
                            onAnswerChange = { value ->
                                viewModel.updateAnswer(question.id, value)
                            }
                        )
                    }
                }

                // Repeating sections (farms)
                if (numberOfFarms > 0) {
                    items(numberOfFarms) { farmIndex ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Farm ${farmIndex + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                viewModel.questions.filter { it.isRepeating }.forEach { question ->
                                    val key = "${question.id}_rep_$farmIndex"
                                    QuestionCard(
                                        question = question,
                                        answer = answers[key],
                                        onAnswerChange = { value ->
                                            viewModel.updateAnswer(question.id, value, farmIndex)
                                        },
                                        repetitionIndex = farmIndex,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Success dialog
    if (uiState is SurveyUiState.Success) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Survey Saved!") },
            text = {
                Text("Survey has been saved locally and will sync when you have internet connection.")
            },
            confirmButton = {
                Button(onClick = {
                    onSurveyCompleted()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun QuestionCard(
    question: Question,
    answer: AnswerValue?,
    onAnswerChange: (AnswerValue) -> Unit,
    modifier: Modifier = Modifier,
    repetitionIndex: Int? = null,
    viewModel: SurveyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()

    // Filter photos for this specific question and repetition
    val relevantPhotos = photos.filter { photo ->
        val photoKey = photo.nameWithoutExtension
        when {
            repetitionIndex != null -> photoKey.contains("_rep_$repetitionIndex")
            else -> !photoKey.contains("_rep_")
        }
    }

    // Create temporary file for camera
    val photoFile = remember(repetitionIndex) {
        createImageFile(context, repetitionIndex)
    }

    val photoUri = remember(photoFile) {
        FileProvider.getUriForFile(
            context,
            "com.pula.surveysync.fileprovider",
            photoFile
        )
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.addPhoto(photoFile)
            onAnswerChange(AnswerValue.TextAnswer(photoFile.absolutePath))
        }
    }

    // Camera permission launcher - launches camera ONLY after permission granted
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            cameraLauncher.launch(photoUri)
        } else {
            // Permission denied
            // TODO: Show user-friendly message
        }
    }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when (question.type) {
                QuestionType.TEXT -> {
                    val textValue = (answer as? AnswerValue.TextAnswer)?.text ?: ""
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { onAnswerChange(AnswerValue.TextAnswer(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter answer...") }
                    )
                }

                QuestionType.NUMBER -> {
                    val numberValue = (answer as? AnswerValue.NumberAnswer)?.number?.toString() ?: ""
                    OutlinedTextField(
                        value = numberValue,
                        onValueChange = {
                            val number = it.toDoubleOrNull()
                            if (number != null) {
                                onAnswerChange(AnswerValue.NumberAnswer(number))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter number...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                QuestionType.SINGLE_CHOICE -> {
                    val selectedOption = (answer as? AnswerValue.MultipleChoiceAnswer)?.choices?.firstOrNull()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = option == selectedOption,
                                    onClick = {
                                        onAnswerChange(AnswerValue.MultipleChoiceAnswer(listOf(option)))
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                QuestionType.PHOTO -> {
                    Button(
                        onClick = {
                            // Request camera permission - camera will launch automatically if granted
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Take Photo")
                    }

                    if (!question.required) {
                        Text(
                            text = "Optional",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Display captured photos for this specific farm/question
                    if (relevantPhotos.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(relevantPhotos) { photoFile ->
                                Box {
                                    Card(
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(photoFile),
                                            contentDescription = "Captured photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removePhoto(photoFile) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove photo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    Text("Unsupported question type")
                }
            }
        }
    }
}

// Helper function to create image file
private fun createImageFile(context: Context, repetitionIndex: Int? = null): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("Pictures")
    val prefix = if (repetitionIndex != null) {
        "SURVEY_${timeStamp}_rep_${repetitionIndex}_"
    } else {
        "SURVEY_${timeStamp}_"
    }
    return File.createTempFile(
        prefix,
        ".jpg",
        storageDir
    )
}

