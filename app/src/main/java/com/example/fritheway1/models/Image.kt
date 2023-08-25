package com.example.fritheway1.models

data class Image(
    val bearing: Double,
    val bottomLeft: Coordinates,
    val bottomRight: Coordinates,
    val center: Coordinates,
    val height: Int,
    val heightMeters: Double,
    val metersToPixels: Double,
    val pixelsToMeters: Double,
    val topLeft: Coordinates,
    val topRight: Coordinates,
    val url: String,
    val width: Int,
    val widthMeters: Double
)