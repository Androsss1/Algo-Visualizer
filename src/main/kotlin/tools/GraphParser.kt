package tools

import model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File





object GraphParser {
    private val json = Json{
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun parse(content: String): Graph {
        try {
            return json.decodeFromString(Graph.serializer(),content)

        } catch(e :Exception){
            throw Exception("Ошибкапарсинга графа: ${e.message}")
        }
    }

    fun parseFile(file: File): Graph {
        if(!file.exists()){
            throw error("File not found: ${file.absolutePath}")
        }
        return parse(file.readText())

    }
}