package com.example.schoolapp

import java.util.*

/**
 * Set the variables for a Communication.
 */
data class CommunicationParents(var title: String, var content: String, var date: Date, var recipients: String) {
    // A row is not expanded by default.
    var isItemExpanded = false
}