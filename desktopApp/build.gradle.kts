import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(compose.material3)

    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        // ADD THIS BLOCK TO DISABLE PROGUARD
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            // Tell the compiler to specifically build an MSI
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Pkg, TargetFormat.Deb, TargetFormat.Rpm)

            // BRUTE-FORCE FIX: Bundle the entire Java runtime so nothing is ever missing!
            includeAllModules = true
            packageName = "TradeMetricsWidget"     // <-- REMOVE THE SPACE HERE
            packageVersion = "1.0.2"
            vendor = "Trade Metrics"
            description = "Private live emotional telemetry widget. Not for public use."
            copyright = "© 2026 Santanu Dutta (santanu.dutta1989@gmail.com). Confidential - Not for public use."
            windows {
                menuGroup = "Trade Metrics"
                iconFile.set(project.file("src/main/resources/icon.ico"))
                shortcut = true
            }
        }
    }
}
