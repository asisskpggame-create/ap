import java.io.File

plugins {
    base
}

fun execCmd(vararg args: String, workDir: File = projectDir) {
    val process = ProcessBuilder(*args)
        .directory(workDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    val code = process.waitFor()
    if (code != 0) {
        throw GradleException("Command failed (${args.joinToString(" ")}):\n$output")
    }
}

fun buildSignedApk(isRelease: Boolean) {
    val sdkRoot = System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_HOME")
        ?: "/opt/android/sdk"
    val buildToolsDir = File(sdkRoot, "build-tools").listFiles()
        ?.filter { it.isDirectory }
        ?.sortedByDescending { it.name }
        ?.firstOrNull()
        ?: throw GradleException("Android build-tools not found under $sdkRoot")
    val platformJar = File(sdkRoot, "platforms/android-36/android.jar").takeIf { it.exists() }
        ?: File(sdkRoot, "platforms").listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.map { File(it, "android.jar") }
            ?.firstOrNull { it.exists() }
        ?: throw GradleException("android.jar not found under $sdkRoot/platforms")

    val distIndex = file("dist/index.html")
    if (!distIndex.exists()) {
        execCmd("npm", "run", "build")
    }

    val workDir = file("build/apk-work/${if (isRelease) "release" else "debug"}")
    delete(workDir)
    val classesDir = File(workDir, "classes").apply { mkdirs() }
    val dexDir = File(workDir, "dex").apply { mkdirs() }
    val genDir = File(workDir, "gen").apply { mkdirs() }
    val assetsDir = File(workDir, "assets/www").apply { mkdirs() }

    copy {
        from("dist")
        into(assetsDir)
    }

    val aapt2 = File(buildToolsDir, "aapt2").absolutePath
    val d8 = File(buildToolsDir, "d8").absolutePath
    val zipalign = File(buildToolsDir, "zipalign").absolutePath
    val apksigner = File(buildToolsDir, "apksigner").absolutePath

    val compiledResZip = File(workDir, "compiled_res.zip")
    execCmd(
        aapt2, "compile",
        "--dir", file("app/src/main/res").absolutePath,
        "-o", compiledResZip.absolutePath
    )

    val unsignedApk = File(workDir, "unaligned.apk")
    execCmd(
        aapt2, "link",
        "-I", platformJar.absolutePath,
        "--manifest", file("app/src/main/AndroidManifest.xml").absolutePath,
        "-A", File(workDir, "assets").absolutePath,
        "--java", genDir.absolutePath,
        "-o", unsignedApk.absolutePath,
        compiledResZip.absolutePath
    )

    val rJava = File(genDir, "com/aistudio/personallifeos/kxmpzq/R.java")
    val mainJava = file("app/src/main/java/com/aistudio/personallifeos/kxmpzq/MainActivity.java")
    execCmd(
        "javac",
        "-source", "11",
        "-target", "11",
        "-classpath", platformJar.absolutePath,
        "-d", classesDir.absolutePath,
        rJava.absolutePath,
        mainJava.absolutePath
    )

    val classFiles = fileTree(classesDir) { include("**/*.class") }.files.map { it.absolutePath }.toTypedArray()
    execCmd(
        d8,
        "--lib", platformJar.absolutePath,
        "--min-api", "24",
        "--output", dexDir.absolutePath,
        *classFiles
    )

    execCmd("jar", "-uf", unsignedApk.absolutePath, "-C", dexDir.absolutePath, "classes.dex")

    val alignedApk = File(workDir, "aligned.apk")
    execCmd(zipalign, "-f", "-p", "4", unsignedApk.absolutePath, alignedApk.absolutePath)

    val debugKeystore = file("debug.keystore")
    if (!debugKeystore.exists() && file("debug.keystore.base64").exists()) {
        val decoded = java.util.Base64.getMimeDecoder().decode(file("debug.keystore.base64").readText())
        debugKeystore.writeBytes(decoded)
    }

    val releaseKeystorePath = System.getenv("KEYSTORE_PATH")?.let { file(it) }
    val useReleaseKey = isRelease && releaseKeystorePath != null && releaseKeystorePath.exists()

    val outApk = if (isRelease) {
        file("app/build/outputs/apk/release/app-release.apk")
    } else {
        file("app/build/outputs/apk/debug/app-debug.apk")
    }
    outApk.parentFile.mkdirs()

    if (useReleaseKey) {
        val storePass = System.getenv("STORE_PASSWORD") ?: "androidrelease"
        val keyPass = System.getenv("KEY_PASSWORD") ?: "androidrelease"
        execCmd(
            apksigner, "sign",
            "--ks", releaseKeystorePath!!.absolutePath,
            "--ks-key-alias", "upload",
            "--ks-pass", "pass:$storePass",
            "--key-pass", "pass:$keyPass",
            "--out", outApk.absolutePath,
            alignedApk.absolutePath
        )
    } else {
        execCmd(
            apksigner, "sign",
            "--ks", debugKeystore.absolutePath,
            "--ks-key-alias", "androiddebugkey",
            "--ks-pass", "pass:android",
            "--key-pass", "pass:android",
            "--out", outApk.absolutePath,
            alignedApk.absolutePath
        )
    }

    if (!isRelease) {
        val backupApk = file(".build-outputs/app-debug.apk")
        backupApk.parentFile.mkdirs()
        outApk.copyTo(backupApk, overwrite = true)
    }

    println("Successfully built and signed Personal Life OS v1.6.0R (versionCode=7) APK: ${outApk.absolutePath} (${outApk.length()} bytes)")
}

tasks.register("testDebugUnitTest") {
    doLast {
        execCmd("npm", "test")
    }
}

tasks.register("assembleDebug") {
    doLast {
        buildSignedApk(isRelease = false)
    }
}

tasks.register("assembleRelease") {
    doLast {
        buildSignedApk(isRelease = true)
    }
}
