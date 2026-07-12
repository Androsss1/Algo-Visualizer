package model

import kotlinx.serialization.Serializable

@Serializable
data class Edge(
    val start: Int,
    val end: Int,
    val weight: Int = 1
)
