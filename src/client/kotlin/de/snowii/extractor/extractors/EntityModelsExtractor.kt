package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.authlib.minecraft.client.MinecraftClient
import de.snowii.extractor.ExtractorClient
import kotlinx.serialization.json.JsonNull
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Display
import org.joml.Vector3f
import org.joml.Vector3fc
import java.lang.reflect.Field

class EntityModelsExtractor : ExtractorClient.ClientExtractor {
    override fun fileName(): String = "entity_models.json"

    override fun extract(client: Minecraft): JsonElement {
        val modelSet = EntityModelSet.vanilla()

        val field: Field = EntityModelSet::class.java.getDeclaredField("roots")
        field.isAccessible = true // Bypass the private modifier

        // Cast the retrieved object to your map
        @Suppress("UNCHECKED_CAST")
        val rootsMap = field.get(modelSet) as Map<net.minecraft.client.model.geom.ModelLayerLocation, net.minecraft.client.model.geom.builders.LayerDefinition>

        val topLevelJson = JsonObject()
        EntityRenderState
        for ((layerLocation, layerDefinition) in rootsMap) {
            // layerDefinition.bakeRoot() returns the ModelPart tree
            val modelPart: ModelPart = layerDefinition.bakeRoot()
            val modelJson = extractModelPart(modelPart)

            // Use the layer location as the key
            topLevelJson.add(layerLocation.toString(), modelJson)
        }

        return topLevelJson
    }

    private fun extractModelPart(part: ModelPart): JsonObject {
        val json = JsonObject()

        // default_transform instead of defaultTransform
        json.add("default_transform", transformToJson(part.defaultTransform))

        // cuboids
        val cuboidArray = JsonArray()
        for (cuboid in part.cuboids) {
            val cuboidJson = JsonObject()
            cuboidJson.add("min", vec3ToJson(cuboid.minX, cuboid.minY, cuboid.minZ))
            cuboidJson.add("max", vec3ToJson(cuboid.maxX, cuboid.maxY, cuboid.maxZ))

            val sidesArray = JsonArray()
            for (side in cuboid.sides) {
                val sideJson = JsonObject()
                sideJson.addProperty("dir", directionToString(side.direction))

                val verticesArray = JsonArray()
                for (vertex in side.vertices) {
                    val vertexJson = JsonObject()
                    vertexJson.add("pos", vec3ToJson(vertex.x, vertex.y, vertex.z))
                    vertexJson.add("uv", vec2ToJson(vertex.u, vertex.v))
                    verticesArray.add(vertexJson)
                }

                sideJson.add("vertices", verticesArray)
                sidesArray.add(sideJson)
            }

            cuboidJson.add("sides", sidesArray)
            cuboidArray.add(cuboidJson)
        }
        json.add("cuboids", cuboidArray)

        // children
        val childrenJson = JsonObject()
        for ((name, child) in part.children) {
            childrenJson.add(name, extractModelPart(child))
        }
        json.add("children", childrenJson)

        return json
    }

    private fun transformToJson(transform: net.minecraft.client.model.ModelTransform): JsonObject {
        return JsonObject().apply {
            add("pivot", vec3ToJson(transform.x, transform.y, transform.z))
            add("rotation", vec3ToJson(transform.pitch, transform.yaw, transform.roll))
            add("scale", vec3ToJson(transform.xScale, transform.yScale, transform.zScale))
        }
    }

    private fun vec3ToJson(x: Float, y: Float, z: Float): JsonArray? {
        if (x.isNaN() || y.isNaN() || z.isNaN()) return null

        return JsonArray().apply {
            add( x)
            add( y)
            add( z)
        }
    }

    private fun vec2ToJson(u: Float, v: Float): JsonArray {
        //if (u.isNaN() || v.isNaN()) return null

        return JsonArray().apply {
            add(u)
            add(v)
        }
    }

    private fun directionToString(dir: Vector3fc): String {
        val x = dir.x()
        val y = dir.y()
        val z = dir.z()

        // Map Minecraft side vectors to Direction enum
        val directionString = when {
            x == 0f && y == -1f && z == 0f -> "Down"
            x == 0f && y == 1f && z == 0f -> "Up"
            x == 0f && y == 0f && z == -1f -> "North"
            x == 0f && y == 0f && z == 1f -> "South"
            x == -1f && y == 0f && z == 0f -> "West"
            x == 1f && y == 0f && z == 0f -> "East"
            else -> "Down"
        }

        return directionString
    }

}
