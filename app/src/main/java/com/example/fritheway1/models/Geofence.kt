package com.example.fritheway1.models

data class Geofence(
    val coordinates: List<Coordinates>,
    val floorNumber: Int,
    val id: String,
    val name: String,
    val payload: Payload
)