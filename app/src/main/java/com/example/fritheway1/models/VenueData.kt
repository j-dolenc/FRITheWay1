package com.example.fritheway1.models

data class VenueData(
    val coordinates: Coordinates,
    val floorPlans: List<FloorPlan>,
    val geofences: List<Geofence>,
    val id: String,
    val name: String,
    val pois: List<Poi>
)