package com.example.a62compass.data.model

/**
 * Data model for a saved heading entry in compass history.
 * Includes pure-Kotlin JSON serialization for DataStore persistence
 * that functions identically on both Android devices and JVM unit tests.
 */
data class SavedHeading(
    val id: Long = System.currentTimeMillis(),
    val heading: Float,
    val direction: String,
    val accuracy: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val escapedDir = direction.replace("\"", "\\\"")
        val escapedAcc = accuracy.replace("\"", "\\\"")
        return """{"id":$id,"heading":$heading,"direction":"$escapedDir","accuracy":"$escapedAcc","timestamp":$timestamp}"""
    }

    companion object {
        fun fromJson(jsonStr: String): SavedHeading {
            val id = Regex(""""id"\s*:\s*(\d+)""").find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
                ?: System.currentTimeMillis()
            val heading = Regex(""""heading"\s*:\s*([0-9.-]+)""").find(jsonStr)?.groupValues?.get(1)?.toFloatOrNull()
                ?: 0f
            val direction = Regex(""""direction"\s*:\s*"([^"]*)"""").find(jsonStr)?.groupValues?.get(1)
                ?: "N"
            val accuracy = Regex(""""accuracy"\s*:\s*"([^"]*)"""").find(jsonStr)?.groupValues?.get(1)
                ?: "Unknown"
            val timestamp = Regex(""""timestamp"\s*:\s*(\d+)""").find(jsonStr)?.groupValues?.get(1)?.toLongOrNull()
                ?: System.currentTimeMillis()

            return SavedHeading(
                id = id,
                heading = heading,
                direction = direction,
                accuracy = accuracy,
                timestamp = timestamp
            )
        }

        fun listToJson(list: List<SavedHeading>): String {
            return "[" + list.joinToString(",") { it.toJson() } + "]"
        }

        fun listFromJson(jsonStr: String): List<SavedHeading> {
            if (jsonStr.isBlank() || jsonStr.trim() == "[]") return emptyList()
            val objectRegex = Regex("""\{[^{}]*\}""")
            return objectRegex.findAll(jsonStr).map { match ->
                fromJson(match.value)
            }.toList()
        }
    }
}
