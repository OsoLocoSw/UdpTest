plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

val COROUTINE_VERSION get() = "1.10.2"
val KOTLIN_VERSION get() = "2.1.0"
val SLF4J2_API_VERSION get() = "2.0.5"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINE_VERSION")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION")
    implementation("org.slf4j:slf4j-api:$SLF4J2_API_VERSION")
}
