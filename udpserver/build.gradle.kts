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
val KOTLIN_VERSION get() = "2.1.0"

dependencies {
    implementation(project(":lib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINE_VERSION")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION")
    implementation(libs.slf4j.simple)
}

application {
    mainModule = "com.babayaga.udpserver"
    mainClass = "com.babayaga.udpserver.MainKt"
}

tasks.withType<Jar> {
    // Otherwise you'll get a "No main manifest attribute" error
    manifest {
        attributes["Main-Class"] = "com.babayaga.udpserver.MainKt"
    }

    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all of the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}