import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "de.fabmax"
    version = "2.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks["jar"].apply {
        this as Jar
        from("$rootDir/LICENSE", "$rootDir/NOTICE.md")
    }
}

// generates the cmake project for building windows platform native libraries
tasks.register("generateNativeProject") {
    group = "native build"

    val os = OperatingSystem.current()
    when {
        os.isWindows -> dependsOn(":physx-jni-natives-windows:generateNativeProjectWindows")
        os.isLinux -> dependsOn(":physx-jni-natives-linux:generateNativeProjectLinux")
        os.isMacOsX -> {
            dependsOn(":physx-jni-natives-macos:generateNativeProjectMacos")
            dependsOn(":physx-jni-natives-macos-arm64:generateNativeProjectMacosArm64")
        }
        else -> throw IllegalStateException("Unsupported OS: $os; for now, only Windows and Linux are supported")
    }

    doFirst {
        if (!File("$projectDir/PhysX/physx").exists()) {
            throw IllegalStateException("Native PhysX project dir does not exist. Run 'git submodule update --init' first")
        }
    }
}

// generates native glue code based on PhysXJs.idl
tasks.register<GenerateNativeGlueCode>("generateNativeGlueCode") {
    idlModelName = "PhysXJni"
    idlSource = File("${projectDir}/physx-jni/src/main/webidl/").absolutePath
    generatorOutput = File("${projectDir}/PhysX/physx/source/webidlbindings/src/jni/").absolutePath
}

tasks.register("buildNativeProject") {
    group = "native build"
    dependsOn("generateNativeGlueCode")

    val os = OperatingSystem.current()
    when {
        os.isWindows -> dependsOn(":physx-jni-natives-windows:buildNativeProjectWindows")
        os.isLinux -> dependsOn(":physx-jni-natives-linux:buildNativeProjectLinux")
        os.isMacOsX -> {
            dependsOn(":physx-jni-natives-macos:buildNativeProjectMacos")
            dependsOn(":physx-jni-natives-macos-arm64:buildNativeProjectMacosArm64")
        }
        else -> throw IllegalStateException("Unsupported OS: $os; for now, only Windows and Linux are supported")
    }
}
