plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
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

    implementation(project(":Library:Remote:Msg:Subscriber:Annotation"))

    implementation(libs.squareup.javapoet)
    implementation(libs.squareup.kotlinpoet)

    implementation(libs.google.auto.service)
    kapt(libs.google.auto.service)

}

