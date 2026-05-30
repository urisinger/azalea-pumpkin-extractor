package de.snowii.extractor

import com.google.gson.GsonBuilder
import com.mojang.authlib.minecraft.client.MinecraftClient
import de.snowii.extractor.extractors.EntityModelsExtractor
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

class ExtractorClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("pumpkin_extractor_client")

    override fun onInitializeClient() {
        logger.info("Starting Pumpkin Extractor (client)")

        val extractors = listOf(
            EntityModelsExtractor()
        )

        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        val outputDir = Files.createDirectories(Paths.get("pumpkin_extractor_output_client"))

        // Wait for Minecraft client to finish initialization
        ClientLifecycleEvents.CLIENT_STARTED.register(ClientLifecycleEvents.ClientStarted { client: Minecraft ->
            for (ext in extractors) {
                try {
                    val outputFile = outputDir.resolve(ext.fileName())
                    Files.newBufferedWriter(outputFile).use { writer ->
                        gson.toJson(ext.extract(client), writer)
                    }
                    logger.info("Wrote client extractor output: ${outputFile.toAbsolutePath()}")
                } catch (e: Exception) {
                    logger.error("Extractor for ${ext.fileName()} failed", e)
                }
            }

            client.stop()
        })
    }

    interface ClientExtractor {
        fun fileName(): String
        fun extract(client: Minecraft): Any
    }
}
