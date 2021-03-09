package com.example.schoolapp

import java.util.*

data class Notification(var title: String?, var content: String?, var date: Date?) {
    // A row is not expanded by default.
    var isItemExpanded = false
}