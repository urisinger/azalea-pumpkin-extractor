package de.snowii.extractor.extractors

import com.google.gson.*
import de.snowii.extractor.ExtractorClient
import net.minecraft.client.Minecraft
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDefinition
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.PartDefinition
import net.minecraft.client.model.geom.builders.UVPair
import net.minecraft.core.Direction
import org.joml.Vector3fc
import java.lang.reflect.Field
import java.util.EnumSet

class EntityModelsExtractor : ExtractorClient.ClientExtractor {
    override fun fileName(): String = "entity_models.json"

    override fun extract(client: Minecraft): JsonElement {
        val modelSet = EntityModelSet.vanilla()
        val rootsMap = getPrivateField<Map<Any, LayerDefinition>>(modelSet, "roots")

        val topLevelJson = JsonObject()

        for ((location, layerDef) in rootsMap) {
            val mesh = getPrivateField<Any>(layerDef, "mesh")
            val rootPart = getPrivateField<PartDefinition>(mesh, "root")

            topLevelJson.add(location.toString(), serializePartDefinition(rootPart))
        }
        return topLevelJson
    }

    private fun serializePartDefinition(part: PartDefinition): JsonObject {
        val pose = getPrivateField<PartPose>(part, "partPose")
        val cubes = getPrivateField<List<CubeDefinition>>(part, "cubes")
        val children = getPrivateField<Map<String, PartDefinition>>(part, "children")

        val json = JsonObject()

        // 1. Transform
        json.add("transform", JsonObject().apply {
            add("pivot", vec3ToJson(pose.x, pose.y, pose.z))
            add("rotation", vec3ToJson(pose.xRot, pose.yRot, pose.zRot))
            add("scale", vec3ToJson(pose.xScale, pose.yScale, pose.zScale))
        })

        // 2. Cubes
        val cubesArray = JsonArray()
        for (cube in cubes) {
            cubesArray.add(serializeCube(cube))
        }
        json.add("cubes", cubesArray)

        // 3. Children (Recursive)
        val childrenJson = JsonObject()
        for ((name, child) in children) {
            childrenJson.add(name, serializePartDefinition(child))
        }
        json.add("children", childrenJson)

        return json
    }

    private fun serializeCube(cube: CubeDefinition): JsonObject {
        val origin = getPrivateField<Vector3fc>(cube, "origin")
        val dimensions = getPrivateField<Vector3fc>(cube, "dimensions")
        val grow = getPrivateField<CubeDeformation>(cube, "grow")
        val visibleFaces = getPrivateField<Set<Direction>>(cube, "visibleFaces")

        // Accessing texture properties
        val texCoord = getPrivateField<UVPair>(cube, "texCoord") // UVPair
        val texScale = getPrivateField<UVPair>(cube, "texScale") // UVPair

        return JsonObject().apply {
            add("origin", vec3ToJson(origin.x(), origin.y(), origin.z()))
            add("dimensions", vec3ToJson(dimensions.x(), dimensions.y(), dimensions.z()))
            add("grow", vec3fromGrow(grow))
            addProperty("mirror", getPrivateField<Boolean>(cube, "mirror"))

            // Adding texture coordinate mapping
            add("tex_coord", vec2FromUVPair(texCoord))
            add("tex_scale", vec2FromUVPair(texScale))

            val faces = JsonArray()
            val order = arrayOf(
                Direction.DOWN, Direction.UP, Direction.NORTH,
                Direction.SOUTH, Direction.WEST, Direction.EAST
            )
            for (dir in order) {
                faces.add(visibleFaces.contains(dir))
            }
            add("visible_faces", faces)
        }
    }

    private fun vec2FromUVPair(uv: UVPair): JsonArray {
        return JsonArray().apply {
            add(uv.u)
            add(uv.v)
        }
    }

    // --- Reflection Helpers ---

    private fun <T> getPrivateField(obj: Any, fieldName: String): T {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(obj) as T
    }

    private fun findField(clazz: Class<*>, name: String): Field {
        var current: Class<*>? = clazz
        while (current != null) {
            try {

            } catch (e: Exception) {
            }
            current = current.superclass
        }
        throw NoSuchFieldException("Field $name not found in ${clazz.name}")
    }

    private fun vec3ToJson(x: Float, y: Float, z: Float) = JsonArray().apply { add(x); add(y); add(z) }

    private fun vec3fromGrow(vec: CubeDeformation): JsonArray {
        // Simple reflection to get x, y, z from Joml/Minecraft vector classes
        val x = getPrivateField(vec, "growX") as Float
        val y = getPrivateField(vec, "growY") as Float
        val z = getPrivateField(vec, "growZ") as Float
        return vec3ToJson(x, y, z)
    }
}