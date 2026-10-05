plugins {
	// Lets Gradle download the JDK requested in build.gradle.kts (Java 25)
	// when the machine running the build does not have it installed.
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "certkit"
