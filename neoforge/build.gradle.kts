plugins {
	id("net.neoforged.moddev") version "2.0.78"
}

dependencies {
	implementation(project.project(":common").sourceSets.getByName("main").output)
}

neoForge {
	version = rootProject.property("neoforge_version").toString()

	runs {
		create("client") {
			client()
			gameDirectory = rootProject.file("run/neoforge-client")
		}
		create("server") {
			server()
			gameDirectory = rootProject.file("run/neoforge-server")
		}
	}

	mods {
		create(rootProject.property("mod_id").toString()) {
			sourceSet(sourceSets.main.get())
		}
	}
}

tasks.jar {
	val commonMain = project(":common").sourceSets.main.get()
	from(commonMain.output.classesDirs)
	from(commonMain.output.resourcesDir)
	destinationDirectory.set(rootDir.resolve("build").resolve("libs_neoforge"))
}

val notNeoTask: (Task) -> Boolean = { !it.name.startsWith("neo") && !it.name.startsWith("compileService") }

tasks.withType<JavaCompile>().matching(notNeoTask).configureEach {
	source(project(":common").sourceSets.main.get().allSource)
}

tasks.withType<Javadoc>().matching(notNeoTask).configureEach {
	source(project(":common").sourceSets.main.get().allSource)
}

tasks.withType<ProcessResources>().matching(notNeoTask).configureEach {
	from(project(":common").sourceSets.main.get().resources)

	val propertyMap = mapOf(
		"mod_id" to rootProject.property("mod_id").toString(),
		"mod_name" to rootProject.property("mod_name").toString(),
		"mod_version" to project.version.toString(),
		"mod_description" to rootProject.property("mod_description").toString(),
		"mod_author" to rootProject.property("mod_author").toString(),
		"mod_license" to rootProject.property("mod_license").toString(),
		"loader_version_range" to rootProject.property("neo_loader_version_range").toString(),
		"minecraft_version_range" to rootProject.property("compatible_minecraft_versions_neo").toString()
	)
	inputs.properties(propertyMap)

	filesMatching("META-INF/neoforge.mods.toml") {
		expand(propertyMap)
	}
}

tasks.named("compileTestJava") {
	enabled = false
}

tasks.named<Test>("test") {
	enabled = false
}
