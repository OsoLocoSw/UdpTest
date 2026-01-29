plugins {
    id("java")
    id("application")
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

dependencies {
    implementation(project(":lib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINE_VERSION")
    implementation(libs.slf4j.simple)
}

application {
    mainModule = "com.babayaga.udpserver"
    mainClass = "com.babayaga.udpserver.Main"
}
