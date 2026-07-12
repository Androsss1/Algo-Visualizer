package model

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val id: Int,
    val x: Float,
    val y: Float,

)