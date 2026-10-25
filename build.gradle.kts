plugins {
    // Loaded once here, so the plugins are not loaded again in every subproject.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidxRoom3) apply false
    alias(libs.plugins.spotless)
}

// The rules of the formatter. Long calls, parameter lists and chains are wrapped one part on each line, trailing commas are kept, and names, file names, comments and the wildcard imports of the iPhone code follow the rules of the project instead of those of ktlint.
val ktlintRules = mapOf(
    "ktlint_code_style" to "intellij_idea",
    "max_line_length" to "120",
    "ktlint_standard_chain-method-continuation" to "enabled",
    "ij_kotlin_allow_trailing_comma" to "true",
    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    "ktlint_standard_no-wildcard-imports" to "disabled",
    "ktlint_standard_function-naming" to "disabled",
    "ktlint_standard_filename" to "disabled",
    "ktlint_standard_property-naming" to "disabled",
    "ktlint_standard_backing-property-naming" to "disabled",
    "ktlint_standard_discouraged-comment-location" to "disabled",
)

// One formatter for the Kotlin of every module and for the build files, run with spotlessApply and checked with spotlessCheck.
spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
        // The rule of line length has to run for long calls to be wrapped, but a line it cannot shorten, such as a long comment, is left as it is.
        suppressLintsFor { shortCode = "standard:max-line-length" }
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
        // The rule of line length has to run for long calls to be wrapped, but a line it cannot shorten, such as a long comment, is left as it is.
        suppressLintsFor { shortCode = "standard:max-line-length" }
    }
}
