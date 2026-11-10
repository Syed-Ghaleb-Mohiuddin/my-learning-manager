plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// Copies the debug APK to dist/ so there is always one known file to sideload.
tasks.register<Copy>("dist") {
    dependsOn(":app:assembleDebug")
    from(layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk"))
    into(layout.projectDirectory.dir("dist"))
    rename { "my-learning-manager.apk" }
}
