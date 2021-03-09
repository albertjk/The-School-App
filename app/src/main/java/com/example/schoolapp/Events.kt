package com.example.schoolapp

import com.google.firebase.firestore.GeoPoint
import java.util.*

/**
 * Set the parameters of an event called Events.
 */
data class Events(
    var title: String,
    var date:Date,
    var startDate:String,
    var endDate:String,
    var description:String,
    var location:String,
    var coordinates: GeoPoint?,
    var shortDescription:String
) {
    // A row is not expanded by default.
    var isItemExpanded = false
}