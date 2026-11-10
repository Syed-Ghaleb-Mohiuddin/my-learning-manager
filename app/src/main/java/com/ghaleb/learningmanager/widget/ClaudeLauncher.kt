package com.ghaleb.learningmanager.widget

import android.content.Context
import android.content.Intent
import android.net.Uri

object ClaudeLauncher {
    private const val PACKAGE = "com.anthropic.claude"

    /** Opens the Claude app, or its Play Store page if it isn't installed. */
    fun intent(context: Context): Intent =
        (context.packageManager.getLaunchIntentForPackage(PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE")))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
