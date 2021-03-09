package com.example.schoolapp

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton

/**
 * AdminHome activity is responsible to present the menu for admin.
 * It includes buttons whose action lead to different functionalities such as
 * Manage students/events/notifications or send communication to parents.
 */
class AdminHomeActivity : AppCompatActivity() {
    // If the current user==0 then the user is admin
    private var currentUser =0

    // Variable which indicates whether the admin wants to forward an event as a message
    var EXTRA_EVENT_MESSAGE: String ="com.example.schoolapp.EXTRA_EVENT_MESSAGE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        // Functionality when log out button is clicked
        val buttonLogOut: ImageButton = findViewById(R.id.logout)
        buttonLogOut.setOnClickListener {
            // Build the dialog with a custom theme (defined in styles.xml)
            val studentsDialog =
                AlertDialog.Builder(this, R.style.AlertDialogTheme)

            // Set the title of the dialog
            studentsDialog.setTitle("Are you sure you want to log out?")

            // Set the cancel button click listener
            studentsDialog.setNeutralButton("No") { dialog, _ ->
                dialog.cancel()
            }

            // Set the positive button click listener
            studentsDialog.setPositiveButton("Yes") { dialog, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
                dialog.dismiss()
            }

            // Create the dialog and show it in screen
            val mDialog = studentsDialog.create()
            mDialog.show()
        }

        // Functionality when the button related with the management of events is clicked
        val button = findViewById<Button>(R.id.button_Manage_events)
        button.setOnClickListener {
            // Start the activity related with the events
            val intent = Intent(
                this,
                EventsActivity::class.java
            )

            // Send the value of the current user in order to know whether the user is the admin or not
            intent.putExtra("currentUser", currentUser.toString())
            startActivity(intent)
        }
        // Functionality when the button related with the communications is clicked
        val buttonManageCommunications = findViewById<Button>(R.id.button_Manage_communication)
        buttonManageCommunications.setOnClickListener {
            // Start the activity related with the communications
            val intent = Intent(
                this, SendCommunicationsActivityPart1::class.java
            )

            /* Send an empty value in order to know that the message has nothing to do with an event.
            It is a new message from the admin. */
            intent.putExtra(EXTRA_EVENT_MESSAGE, "")
            startActivity(intent)
        }
        // Functionality when the button related with the notifications is clicked
        val buttonManageNotifications = findViewById<Button>(R.id.button_Manage_notifications)
        buttonManageNotifications.setOnClickListener {
            // Start the activity related with the notifications
            val intent = Intent(
                this, ManageNotificationActivity::class.java
            )
            intent.putExtra("currentUser", currentUser.toString())
            startActivity(intent)
        }
        // Functionality when the button related with the students is clicked
        val buttonManageStudents = findViewById<Button>(R.id.button_ManageStudents)
        buttonManageStudents.setOnClickListener {
            // Start the activity related with the students
            val intent = Intent(
                this, ManageStudentActivity::class.java
            )
            intent.putExtra("currentUser", currentUser.toString())
            startActivity(intent)
        }
    }
}