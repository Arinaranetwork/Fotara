plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val appVersionCode = 72
val appVersionName = "2.0.0"
val appChannel = when {
    appVersionName.contains("alpha", ignoreCase = true) -> "alpha"
    appVersionName.contains("beta", ignoreCase = true) -> "beta"
    else -> "stable"
}

android {
    namespace = "com.arinara.fotara"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.arinara.fotara"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "VERSION_CHANNEL", "\"$appChannel\"")
    }

    androidResources {
        localeFilters += listOf("en")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    sourceSets {
        getByName("main") {
            assets.directories += file("build/generated/assets/legal").path
        }
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

abstract class VerifyAndroidRulesTask : DefaultTask() {
    @get:InputDirectory
    abstract val uiSourceDir: DirectoryProperty

    @get:InputFile
    @get:Optional
    abstract val notifyScript: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val auditOnly: Property<Boolean>

    @TaskAction
    fun verify() {
        val uiDir = uiSourceDir.get().asFile
        if (!uiDir.exists()) return

        val violations = mutableListOf<String>()
        val allowedRadii = setOf(0, 12, 14, 16, 20, 24, 28, 999)
        val colorRegex = Regex("""\bColor\s*\(\s*0x[0-9a-fA-F]+""")
        val rawColorConstantRegex = Regex("""\bColor\.(Red|Green|Blue|Yellow|Cyan|Magenta|Black|DarkGray|LightGray)\b""")
        val rawComponentRegex = Regex("""(^|\s)(androidx\.compose\.material3\.)?(Button|Card|OutlinedButton|TextButton|ElevatedButton|FloatingActionButton|Slider)\s*\(""")
        val rawPopupRegex = Regex("""(^|\s)(androidx\.compose\.material3\.)?(AlertDialog|ModalBottomSheet)\s*\(""")
        val rawDialogRegex = Regex("""(^|\s)(androidx\.compose\.ui\.window\.)?Dialog\s*\(""")
        val shapeRegex = Regex("""RoundedCornerShape\s*\(\s*([0-9]+)\.dp\s*\)""")
        val pointerRegex = Regex("""\.pointerInput\s*\(\s*([^)]+)\s*\)""")
        val arbitraryBorderRegex = Regex("""\.border\s*\(\s*([0-9.]+)\.dp\s*,""")
        val allowedBorderWidths = setOf(0.5, 1.0, 2.0)
        val hardcodedFontSizeRegex = Regex("""fontSize\s*=\s*([0-9]+)\.sp""")
        val allowedFontSizes = setOf(10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 40)
        val rawShadowRegex = Regex("""\.(shadow|dropShadow)\s*\(""")
        val topSpacerRegex = Regex("""Spacer\s*\(\s*modifier\s*=\s*Modifier\.height\s*\(\s*([0-9]+)\.dp\s*\)\s*\)""")

        val ktFiles = uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        for (file in ktFiles) {
            val relPath = file.relativeTo(uiDir).path.replace('\\', '/')
            val isThemeOrToken = relPath.contains("theme/") || relPath.endsWith("DesignTokens.kt") || relPath.endsWith("Color.kt") || relPath.endsWith("Theme.kt") || relPath.endsWith("Shape.kt") || relPath.endsWith("Typography.kt")
            val isPreset = relPath.startsWith("components/presets/")
            val isScreen = file.name.endsWith("Screen.kt")
            val isDialogOrSheet = file.name.contains("Dialog") || file.name.contains("Sheet") || file.name.contains("Modal")

            val fileText = file.readText()
            val lines = file.readLines()
            var fileHasBottomClearance = false
            var fileHasHardwareClearance = false

            // AR-012: Prohibit White Component Borders (single-line and multi-line calls)
            if (!isThemeOrToken) {
                var searchIdx = 0
                while (searchIdx < fileText.length) {
                    val borderPos = fileText.indexOf(".border(", searchIdx)
                    val borderStrokePos = fileText.indexOf("BorderStroke(", searchIdx)
                    val nextCallPos = when {
                        borderPos != -1 && borderStrokePos != -1 -> minOf(borderPos, borderStrokePos)
                        borderPos != -1 -> borderPos
                        borderStrokePos != -1 -> borderStrokePos
                        else -> -1
                    }
                    if (nextCallPos == -1) break

                    val openParen = fileText.indexOf('(', nextCallPos)
                    if (openParen == -1) break

                    var depth = 1
                    var closeParen = openParen + 1
                    while (depth > 0 && closeParen < fileText.length) {
                        if (fileText[closeParen] == '(') depth++
                        else if (fileText[closeParen] == ')') depth--
                        closeParen++
                    }

                    val callContent = fileText.substring(openParen + 1, minOf(closeParen - 1, fileText.length))
                    if (callContent.contains("Color.White") || 
                        callContent.contains("0xFFFFFFFF") || 
                        callContent.contains("0xFFFFFF,") || 
                        callContent.contains("#FFFFFF", ignoreCase = true) ||
                        (callContent.contains("TextPrimary") && callContent.contains("copy"))) {
                        val lineNum = fileText.substring(0, nextCallPos).count { it == '\n' } + 1
                        violations.add("[$relPath:$lineNum] [AR-012: Prohibit White Component Borders] Component border uses white color. Borders must strictly resolve to BorderSubtle (#262F3E) or PrimaryAccent (#7A5AF8).")
                    }
                    searchIdx = closeParen
                }
            }

            // AR-016: Compact Top Layer Spacing (no vertical spacers > 24dp in screens before bottom clearance)
            if (isScreen && !isPreset) {
                for (match in topSpacerRegex.findAll(fileText)) {
                    val height = match.groupValues[1].toIntOrNull() ?: 0
                    if (height in 25..95) {
                        val lineNum = fileText.substring(0, match.range.first).count { it == '\n' } + 1
                        violations.add("[$relPath:$lineNum] [AR-016: Compact Top Layer Spacing] Excessive vertical space of ${height}.dp. Vertical spacers in heading/top layers must not exceed 24.dp.")
                    }
                }
            }

            // AR-014: Settings Selection Rectangle Aspect Ratio
            if (file.name.contains("BorderPicker") || (file.name == "SettingsScreen.kt" && relPath.contains("settings"))) {
                if (fileText.contains("LazyVerticalGrid") && fileText.contains("AppCard") && !fileText.contains("aspectRatio(1f)")) {
                    violations.add("[$relPath] [AR-014: Settings Selection Rectangle Aspect Ratio] Selection cards in settings must enforce equal width and height (aspectRatio(1f)).")
                }
            }

            // Hardware UI Inset check
            if (fileText.contains("AppScreen") || 
                fileText.contains("statusBarsPadding()") || 
                fileText.contains("systemBarsPadding()") || 
                fileText.contains("safeDrawingPadding()") || 
                fileText.contains("AppTopBar") || 
                fileText.contains("TopAppBar") || 
                fileText.contains("statusBars") || 
                fileText.contains("safeDrawing")) {
                fileHasHardwareClearance = true
            }

            lines.forEachIndexed { idx, line ->
                val lineNum = idx + 1
                val trimmed = line.trim()

                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                    return@forEachIndexed
                }

                if (trimmed.contains("AppScreen") || trimmed.contains("100.dp") || trimmed.contains("DefaultBottomClearance") || trimmed.contains("BottomClearanceSpacer")) {
                    fileHasBottomClearance = true
                }

                // AR-001: Strict Token Adherence (No hardcoded colors)
                if (!isThemeOrToken && colorRegex.containsMatchIn(line)) {
                    val m = colorRegex.find(line)?.value ?: "Color(0x...)"
                    violations.add("[$relPath:$lineNum] [AR-001: Strict Token Adherence] Hardcoded color '$m'. Must resolve to Electric Slate tokens in DesignTokens.kt.")
                }

                // AR-005: Max 3 Active Colors / Too Much Color (Rainbow Prevention)
                if (!isThemeOrToken && rawColorConstantRegex.containsMatchIn(line)) {
                    val m = rawColorConstantRegex.find(line)?.value ?: "Color.X"
                    violations.add("[$relPath:$lineNum] [AR-005: Too Much Color / Standard Color Constant] Raw '$m' used. Prohibit ad-hoc colors. Must resolve to Electric Slate tokens (PrimaryAccent, Destructive, TextPrimary, TextMuted) in DesignTokens.kt.")
                }

                // AR-003: Mandatory Component Presets
                if (!isPreset && rawComponentRegex.containsMatchIn(line)) {
                    val comp = rawComponentRegex.find(line)?.groupValues?.get(3) ?: "Component"
                    violations.add("[$relPath:$lineNum] [AR-003: Mandatory Component Presets] Raw Compose component '$comp' used. Must use standardized preset from ui.components.presets (AppButton, AppCard, AppFAB, AppSlider).")
                }

                // AR-009: Mandatory Modal & Popup Presets (No raw AlertDialog or unstyled popups)
                if (!isPreset && !isDialogOrSheet) {
                    if (rawPopupRegex.containsMatchIn(line)) {
                        val popup = rawPopupRegex.find(line)?.groupValues?.get(3) ?: "Modal"
                        violations.add("[$relPath:$lineNum] [AR-009: Mandatory Modal Presets] Raw '$popup' used. Modals must use AppBottomSheet (with 28.dp top corners and minimize-to-pill) or ConnectedInspector.")
                    } else if (rawDialogRegex.containsMatchIn(line)) {
                        violations.add("[$relPath:$lineNum] [AR-009: Mandatory Modal Presets] Raw 'Dialog' used. Modals must use AppBottomSheet or ConnectedInspector.")
                    }
                }

                // AR-004: Standardized Shape Hierarchy
                if (!isThemeOrToken && shapeRegex.containsMatchIn(line)) {
                    val matches = shapeRegex.findAll(line)
                    for (match in matches) {
                        val radius = match.groupValues[1].toIntOrNull()
                        if (radius != null && radius !in allowedRadii) {
                            violations.add("[$relPath:$lineNum] [AR-004: Standardized Shape Hierarchy] Arbitrary RoundedCornerShape(${radius}.dp). Must resolve to standard tokens: 999.dp (Capsule), 28.dp (Sheet), 24.dp (Card), 20.dp/16.dp (Panel), 12.dp/14.dp (Row), 0.dp (Flat), or CircleShape.")
                        }
                    }
                }

                // AR-008: Standardized Outlines & Image Borders
                if (!isThemeOrToken && arbitraryBorderRegex.containsMatchIn(line)) {
                    val matches = arbitraryBorderRegex.findAll(line)
                    for (match in matches) {
                        val width = match.groupValues[1].toDoubleOrNull()
                        if (width != null && width !in allowedBorderWidths) {
                            violations.add("[$relPath:$lineNum] [AR-008: Standardized Outlines & Image Borders] Inconsistent border width '${width}.dp'. Outlines and photo borders must use 1.dp (BorderSubtle) or 2.dp for active focus/selection rings.")
                        }
                    }
                }

                // AR-010: Standardized Typography Hierarchy
                if (!isThemeOrToken && hardcodedFontSizeRegex.containsMatchIn(line)) {
                    val matches = hardcodedFontSizeRegex.findAll(line)
                    for (match in matches) {
                        val size = match.groupValues[1].toIntOrNull()
                        if (size != null && size !in allowedFontSizes) {
                            violations.add("[$relPath:$lineNum] [AR-010: Standardized Typography Hierarchy] Arbitrary font size '${size}.sp'. Must resolve to standard typography scale tokens in Typography.kt.")
                        }
                    }
                }

                // AR-011: Strict Elevation & Shadow Tokens
                if (!isThemeOrToken && rawShadowRegex.containsMatchIn(line)) {
                    violations.add("[$relPath:$lineNum] [AR-011: Strict Elevation & Shadow Tokens] Raw drop shadow modifier used. Dirty drop shadows are prohibited; use Material 3 tonal surface elevation and 1dp BorderSubtle.")
                }

                // AR-002: Stable PointerInput Keys
                if (pointerRegex.containsMatchIn(line)) {
                    val keyArg = pointerRegex.find(line)?.groupValues?.get(1)?.trim() ?: ""
                    if (keyArg != "Unit" && (keyArg.contains(",") || keyArg.contains("offset", ignoreCase = true) || keyArg.contains("scale", ignoreCase = true) || keyArg.contains("pan", ignoreCase = true) || keyArg.contains("zoom", ignoreCase = true))) {
                        violations.add("[$relPath:$lineNum] [AR-002: Stable PointerInput Keys] Unstable pointerInput key '$keyArg'. Must pass Unit or stable key to prevent gesture restart stutter.")
                    }
                }
            }

            // AR-006: Safe Bottom Page Clearance
            if (isScreen && !isPreset && !fileHasBottomClearance && !file.name.contains("Dialog") && !file.name.contains("Sheet") && !file.name.contains("Crop")) {
                violations.add("[$relPath] [AR-006: Safe Bottom Page Clearance] Screen composable does not enforce >= 100.dp bottom clearance. Wrap in AppScreen or provide contentPadding with bottom >= 100.dp.")
            }

            // AR-015: Hardware UI Clearance & Overflow Prevention
            if (isScreen && !isPreset && !fileHasHardwareClearance && !file.name.contains("Dialog") && !file.name.contains("Sheet") && !file.name.contains("Crop")) {
                violations.add("[$relPath] [AR-015: Hardware UI Clearance & Overflow Prevention] Screen does not enforce safe hardware insets. Must use AppScreen, statusBarsPadding(), or AppTopBar to prevent content overflow behind hardware UI.")
            }
        }

        if (violations.isNotEmpty()) {
            val total = violations.size
            val fileCount = violations.map { it.substringBefore(':') }.distinct().size
            println("\n" + "=".repeat(80))
            println("❌ ANDROID.MD RULES VIOLATION REPORT - $total VIOLATION(S) ACROSS $fileCount FILE(S)")
            println("=".repeat(80))
            violations.forEach { println("  • $it") }
            println("=".repeat(80) + "\n")

            val script = notifyScript.orNull?.asFile
            if (script != null && script.exists()) {
                try {
                    val alertCmd = ProcessBuilder("cmd.exe", "/c", script.absolutePath, "Android.md Rules Violation", "Build canceled: Found $total violations across $fileCount files.")
                    alertCmd.inheritIO().start().waitFor()
                } catch (_: Exception) {}
            }

            if (auditOnly.getOrElse(false) != true) {
                throw GradleException("Build canceled: $total Android.md rule violation(s) detected across $fileCount file(s). Comply with Android.md to proceed.")
            }
        } else {
            println("\n✅ [verifyAndroidRules] All UI screens fully comply with Android.md design tokens, presets, and physics!\n")
        }
    }
}

val copyLegalDocsToAssets = tasks.register<Copy>("copyLegalDocsToAssets") {
    from(rootProject.file("Legal")) {
        include("PRIVACY.md", "TERMS.md")
    }
    into(file("build/generated/assets/legal"))
}

val verifyAndroidRules = tasks.register<VerifyAndroidRulesTask>("verifyAndroidRules") {
    uiSourceDir.set(file("src/main/java/com/arinara/fotara/ui"))
    notifyScript.set(rootProject.file(".agents/scripts/notify.cmd"))
    auditOnly.set(project.hasProperty("auditAndroidRulesOnly"))
}

tasks.named("preBuild").configure {
    dependsOn(copyLegalDocsToAssets)
    dependsOn(verifyAndroidRules)
}

tasks.matching { it.name in listOf("preDebugUnitTestBuild", "testDebugUnitTest", "assembleDebug", "assembleRelease", "check") }.configureEach {
    dependsOn(verifyAndroidRules)
}



kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)

  // CameraX
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.concurrent.futures.ktx)

  // ML Kit On-Device Text Recognition
  implementation(libs.mlkit.text.recognition)

  // Coil Image Loading
  implementation(libs.coil.compose)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.json:json:20240303")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Jetpack Glance Widget
  implementation("androidx.glance:glance-appwidget:1.1.1")
  implementation("androidx.glance:glance-material3:1.1.1")

  // Chrome Custom Tabs / Browser Link Support
  implementation("androidx.browser:browser:1.8.0")

  // Coil GIF decoding
  implementation("io.coil-kt:coil-gif:2.7.0")

  // Biometric Authentication
  implementation("androidx.biometric:biometric:1.2.0-alpha05")

  // Shizuku Privileged Shell Downgrade API
  implementation("dev.rikka.shizuku:api:13.1.5")
  implementation("dev.rikka.shizuku:provider:13.1.5")
}

abstract class PrintVersionTask : DefaultTask() {
    @get:Input
    abstract val vName: Property<String>
    @get:Input
    abstract val vCode: Property<Int>
    @get:Input
    abstract val vChan: Property<String>

    @TaskAction
    fun run() {
        println("VERSION_NAME=${vName.get()}")
        println("VERSION_CODE=${vCode.get()}")
        println("VERSION_CHANNEL=${vChan.get()}")
    }
}

tasks.register<PrintVersionTask>("printVersion") {
    description = "Outputs the current dynamic project version and release channel."
    vName.set(appVersionName)
    vCode.set(appVersionCode)
    vChan.set(appChannel)
}

