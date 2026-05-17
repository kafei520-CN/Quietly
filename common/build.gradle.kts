plugins {
	id("fabric-loom") version "1.16-SNAPSHOT"
}

dependencies {
	minecraft("com.mojang:minecraft:${rootProject.property("minecraft_version")}")
	mappings(loom.officialMojangMappings())

	modImplementation("net.fabricmc:fabric-loader:${rootProject.property("loader_version")}")
	compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
	annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
	compileOnly("net.fabricmc:sponge-mixin:0.15.3+mixin.0.8.7")
}

loom {
	mixin.useLegacyMixinAp = false
}

tasks.jar {
	enabled = false
}

tasks.named("remapJar") {
	enabled = false
}
