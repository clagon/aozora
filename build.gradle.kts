plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// Kotlin の整形は ktlint で行う（規則は .editorconfig）。VS Code の保存時とエージェントの編集後にも同じものを走らせる（AGENTS.md）
val ktlint by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
}

// -PktlintFiles=a.kt,b.kt で対象を絞れる。指定がなければ全体
val ktlintTargets = providers.gradleProperty("ktlintFiles").map { it.split(',') }
    .orElse(listOf("app/src/**/*.kt", "*.kts", "app/*.kts"))

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Kotlin のコードが整形済みかを確かめる"
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    args(ktlintTargets.get())
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Kotlin のコードを整形する"
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(listOf("-F") + ktlintTargets.get())
}
