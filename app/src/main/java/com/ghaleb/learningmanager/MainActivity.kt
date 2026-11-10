package com.ghaleb.learningmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.ghaleb.learningmanager.ui.AppRoot
import com.ghaleb.learningmanager.ui.theme.LearningTheme
import com.ghaleb.learningmanager.widget.WidgetUpdater
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val syncError = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LearningTheme {
                AppRoot(syncError.value, ::sync)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sync()
    }

    private fun sync() {
        lifecycleScope.launch {
            syncError.value = repository.sync().exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }
            WidgetUpdater.refresh(applicationContext)
        }
    }
}
