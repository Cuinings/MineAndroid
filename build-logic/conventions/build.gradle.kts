plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("gradle-plugin"))
    implementation("com.android.tools.build:gradle:8.9.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
