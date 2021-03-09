package com.example.schoolapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton

/**
 * StudentHome activity is responsible to present the menu for student.
 * It includes buttons whose action lead to different functionalities such as
 * view profile/events/notifications.
 */
class StudentHomeActivity : AppCompatActivity() {
    //variable with the student's email
    var EXTRA_STUDENT_PROFILE : String = "com.example.schoolapp.EXTRA_STUDENT_PROFILE"
    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_home)
        // From the intent, get the e-mail address of the student who logged in.
        val studentEmail : String? = intent.getStringExtra(com.example.schoolapp.MainActivity().EXTRA_STUDENT_HOME)
        //if the current user==0 then the user is admin
        var currentUser:String? = intent.getStringExtra("currentUser")
        //functionality when log out button is clicked
        val buttonLogOut:ImageButton = findViewById(R.id.logout)
        buttonLogOut.setOnClickListener {
            //build the dialog with a custom theme (defined in styles.xml)
            val studentsDialog =
                AlertDialog.Builder(this, R.style.AlertDialogTheme)
            //set the title of the dialog
            studentsDialog.setTitle("Are you sure you want to log out?")
            // Set the cancel button click listener
            studentsDialog.setNeutralButton("No") { dialog, _ ->
                dialog.cancel()
            }
            //Set the positive button click listener
            studentsDialog.setPositiveButton("Yes") { dialog, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
                dialog.dismiss()
            }
            //create the dialog and show it in screen
            val mDialog = studentsDialog.create()
            mDialog.show()
        }
        //find the button of events
        val button :Button = findViewById<Button>(R.id.button_events)
        //if 'Events' is clicked moved to another acitvity to show the events
        button.setOnClickListener{
            val intent = Intent(this, EventsActivity::class.java).apply {
                //send the value of the current user in order to know whether the user is the admin or not
                putExtra("currentUser", currentUser.toString())
                //send the student's email to the new activity
                putExtra(EXTRA_STUDENT_PROFILE, studentEmail)
            }
            startActivity(intent)
        }
        //find the button of notifications
        val buttonNotifications :Button = findViewById<Button>(R.id.button_notifications_student)
        //if 'Notifications' is clicked moved to another activity to show the notifications
        buttonNotifications.setOnClickListener{
            val intent = Intent(this, NotificationsActivity::class.java).apply {
                //send the value of the current user in order to know whether the user is the admin or not
                putExtra("currentUser", currentUser.toString())
                //send the student's email to the new activity
                putExtra(EXTRA_STUDENT_PROFILE, studentEmail)
            }
            startActivity(intent)
        }
        //find the button of view profile
        val viewProfileButton : Button = findViewById(R.id.button_viewProfile)
        //id 'View Profile' is clicked moved to another activity to show the information of the user
        viewProfileButton.setOnClickListener{
            val intent = Intent(this, StudentProfileActivity::class.java).apply {
                //send the student's email to the new activity
                putExtra(EXTRA_STUDENT_PROFILE, studentEmail)
            }
            startActivity(intent)
        }
    }
}
