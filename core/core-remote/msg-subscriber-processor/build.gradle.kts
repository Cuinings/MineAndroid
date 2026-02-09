plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {

    implementation(project(":core:core-remote:msg-subscriber-annotation"))

    implementation(libs.squareup.javapoet)
    implementation(libs.squareup.kotlinpoet)

    implementation(libs.google.auto.service)
    kapt(libs.google.auto.service)

}
