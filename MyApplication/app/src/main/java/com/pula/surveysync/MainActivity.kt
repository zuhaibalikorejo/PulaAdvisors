package com.pula.surveysync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pula.surveysync.data.local.entity.FarmerEntity
import com.pula.surveysync.presentation.ui.FarmerSelectionScreen
import com.pula.surveysync.presentation.ui.SurveyQuestionnaireScreen
import com.pula.surveysync.presentation.ui.SyncScreen
import com.pula.surveysync.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen {
    object FarmerSelection : Screen()
    data class SurveyQuestionnaire(val farmer: FarmerEntity) : Screen()
    object SyncStatus : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.FarmerSelection) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.FarmerSelection -> {
                            FarmerSelectionScreen(
                                onFarmerSelected = { farmer ->
                                    currentScreen = Screen.SurveyQuestionnaire(farmer)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is Screen.SurveyQuestionnaire -> {
                            SurveyQuestionnaireScreen(
                                farmer = screen.farmer,
                                onBack = {
                                    currentScreen = Screen.FarmerSelection
                                },
                                onSurveyCompleted = {
                                    currentScreen = Screen.SyncStatus
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is Screen.SyncStatus -> {
                            SyncScreen(
                                onNewSurvey = {
                                    currentScreen = Screen.FarmerSelection
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
