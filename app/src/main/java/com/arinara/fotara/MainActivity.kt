// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.AppContainer
import com.arinara.fotara.data.DefaultAppContainer
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.theme.FolderBodyBlue
import com.arinara.fotara.theme.FotaraTheme
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TextSecondary
import java.io.File
import java.util.Date

class MainActivity : ComponentActivity() {

    private var appContainer: AppContainer? by mutableStateOf(null)
    private var startupError: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install local crash logger to capture any uncaught exception to disk
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val logFile = File(filesDir, "crash.log")
                logFile.writeText(
                    "FOTARA FATAL CRASH [${Date()}]\n" +
                    "Thread: ${thread.name}\n" +
                    Log.getStackTraceString(throwable)
                )
            } catch (_: Exception) {}
            previousHandler?.uncaughtException(thread, throwable)
        }

        initializeContainer()

        enableEdgeToEdge()
        val deepLinkPhotoId = intent?.getLongExtra("photo_id", -1L)?.takeIf { it > 0 }
        val deepLinkDirectView = intent?.getBooleanExtra("direct_view_note", false) ?: false

        setContent {
            FotaraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightNavy
                ) {
                    val container = appContainer
                    val error = startupError
                    when {
                        container != null -> {
                            MainNavigation(
                                appContainer = container,
                                deepLinkPhotoId = deepLinkPhotoId,
                                deepLinkDirectView = deepLinkDirectView
                            )
                        }
                        error != null -> {
                            StartupRecoveryScreen(
                                errorMessage = error,
                                onResetAndRestart = {
                                    try {
                                        deleteDatabase(FotaraDbHelper.DATABASE_NAME)
                                    } catch (_: Exception) {}
                                    startupError = null
                                    initializeContainer()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initializeContainer() {
        try {
            appContainer = DefaultAppContainer(applicationContext)
            startupError = null
        } catch (e: Throwable) {
            Log.e("MainActivity", "Failed to initialize AppContainer: ${e.message}", e)
            startupError = e.message ?: e.toString()
        }
    }
}

@Composable
private fun StartupRecoveryScreen(
    errorMessage: String,
    onResetAndRestart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TagAmber,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Fotara Recovery",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "An initialization error occurred:\n$errorMessage",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onResetAndRestart,
            colors = ButtonDefaults.buttonColors(containerColor = FolderBodyBlue)
        ) {
            Text("Reset Database & Restart", color = Color.White)
        }
    }
}
