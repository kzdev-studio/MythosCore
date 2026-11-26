plugins {
    id("fabric-loom") version "1.9.2"
    id("java")
}

group = "pl.mythoscore"
version = "1.0.0-alpha"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven { name = "Fabric"; url = uri("https://maven.fabricmc.net/") }
    maven { name = "Spigot"; url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
    maven { name = "JitPack"; url = uri("https://jitpack.io") }
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.1")
    mappings("net.fabricmc:yarn:1.20.1+build.10:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.14")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.92.6+1.20.1")

    // Spigot API - PAKUJEMY DO JARA
    implementation("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    include("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    // SnakeYAML - PAKUJEMY DO JARA
    implementation("org.yaml:snakeyaml:2.2")
    include("org.yaml:snakeyaml:2.2")

    // Vault API - PAKUJEMY DO JARA
    implementation("com.github.MilkBowl:VaultAPI:1.7") { exclude(group = "org.bukkit", module = "bukkit") }
    include("com.github.MilkBowl:VaultAPI:1.7") { exclude(group = "org.bukkit", module = "bukkit") }

    // ASM – NIE PAKUJEMY DO JARA (Fabric ma własne ASM)
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
}