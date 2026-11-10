package com.ghaleb.learningmanager

import android.app.Application
import android.content.Context
import com.ghaleb.learningmanager.data.LearningDatabase
import com.ghaleb.learningmanager.data.LearningRepository
import com.ghaleb.learningmanager.data.SettingsStore
import com.ghaleb.learningmanager.data.SupabaseApi
import com.ghaleb.learningmanager.data.appPrefs
import com.ghaleb.learningmanager.reminder.ReminderScheduler
import com.ghaleb.learningmanager.widget.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LearningApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: LearningRepository by lazy {
        val api = if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            SupabaseApi(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        } else {
            null
        }
        LearningRepository(api, LearningDatabase.create(this).dao(), appPrefs)
    }

    val settings: SettingsStore by lazy { SettingsStore(appPrefs) }

    override fun onCreate() {
        super.onCreate()
        WidgetRefreshWorker.schedule(this)
        appScope.launch { ReminderScheduler.schedule(this@LearningApp, settings.current()) }
    }
}

val Context.repository: LearningRepository get() = (applicationContext as LearningApp).repository
val Context.settings: SettingsStore get() = (applicationContext as LearningApp).settings
