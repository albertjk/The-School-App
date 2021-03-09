package com.example.schoolapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * ParentsHome activity is responsible to present the menu for parents.
 * It includes buttons whose action lead to different functionalities such as
 * view kid's profile, view events/notifications/communications.
 */
class ParentsHomeActivity : AppCompatActivity() {
    //if the current user==0 then the user is admin
    private var currentUser = 1
    var EXTRA_STUDENT_PROFILE : String = "com.example.schoolapp.EXTRA_STUDENT_PROFILE"
    var EXTRA_PARENT_HOME: String = "com.example.schoolapp.EXTRA_PARENT_HOME"
    // Access a Cloud Firestore instance
    val db = Firebase.firestore
    val db2 = Firebase.firestore
    //variable used to print messages in Log
    var TAG = "Log.TAG"
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parents_home)
        //functionality when log out button is clicked
        val buttonLogOut: ImageButton = findViewById(R.id.logout)
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

        /**
         * If 'Events' button is clicked , then the activity related to the events
         * representation begins.
         */
        val button = findViewById<Button>(R.id.button_events)
        button.setOnClickListener {
            //start the activity related with the events
            val intent = Intent(
                this,
                EventsActivity::class.java
            )
            //send the value of the current user in order to know whether the user is the admin or not
            intent.putExtra("currentUser", currentUser.toString())
            startActivity(intent)
        }
        /**
         * If 'Notifications' button is clicked , then the activity related to the notifications
         * representation begins.
         */
        val buttonNotifications = findViewById<Button>(R.id.button_notifications_parents)
        buttonNotifications.setOnClickListener {
            //start the activity related with the notifications
            val intent = Intent(this, NotificationsActivity::class.java)
            //send the value of the current user in order to know whether the user is the admin or not
            intent.putExtra("currentUser", currentUser.toString())
            startActivity(intent)
        }

        /**
         * If 'Communication with school' button is clicked, then the activity related to the communication
         * with school begins.
         */
        val buttonCommunication = findViewById<Button>(R.id.button_communication)
        buttonCommunication.setOnClickListener {
            //start the activity related with the communications
            val intent = Intent(
                this,
                CommunicationParentsActivity::class.java
            ).apply {
                //send the username of parent
                putExtra(EXTRA_PARENT_HOME, intent.getStringExtra(EXTRA_PARENT_HOME))
            }
            startActivity(intent)
        }
        /**
         * If 'View my Kids profile' button is clicked, then the activity related to the information
         * of user's kids begins.
         */
        val buttonViewKids = findViewById<Button>(R.id.button_viewProfileParent)
        buttonViewKids.setOnClickListener {
            btnClickedViewProfileParent()
        }
    }
    /**
     * This function is called when the button of 'View my kid's profile'
     * in Parent's HomePage is clicked.
     * A dialog with a list of parent's kids appeared and a parent has to
     * choose one of them and click 'View' in order to view his/her profile
     * or cancel if he/she changes his/her mind.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun btnClickedViewProfileParent() {
        //the list with the name of kids (students) the parent has
        var name: List<String>? = null
        var listItemsToShow: Array<String?>? = null
        var kidsNames : HashMap<Int, String>
                = HashMap<Int, String> ()
        var parentUsername=intent.getStringExtra(EXTRA_PARENT_HOME)
        // Find the parent based on their username, which is their unique identifier.
        db.collection("parents")
            .whereEqualTo("username", parentUsername.toString())
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    name = document.data["kids"] as List<String>
                }
                var listItems = name?.size?.let { arrayOfNulls<String>(it) }
                //find his/her kids in students collection
                db2.collection("students")
                    .get()
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }
                    .addOnSuccessListener { documents ->
                        var counter = 0
                        for (i in name!!) {
                            //for each student
                            for (document in documents) {
                                var temp: String? = null
                                //if the student is a kid of the current parent
                                if (document.id == i) {
                                    // Iterate over each data item about the student
                                    for (item in document.data) {
                                        //receive his/her name
                                        if (item.key == "first name") {
                                            temp = item.value.toString()
                                        }
                                        if (item.key == "last Name") {
                                            temp = temp + " " + item.value.toString()
                                        }
                                    }
                                    //create a list with the kids names
                                    if (temp != null) {
                                        listItems?.set(counter, temp.toString())
                                        kidsNames.put(counter,i)
                                        counter += 1
                                    }
                                }
                            }
                        }
                    }
                    .addOnCompleteListener() {
                        listItemsToShow = listItems?.copyOf()
                //build the dialog with a custom theme (defined in styles.xml)
                val studentsDialog =
                    AlertDialog.Builder(this, R.style.AlertDialogTheme)
                        var selectedValue: Int =-1
                //set the title of the dialog
                studentsDialog.setTitle("Choose a student")
                //set the choices of the dialog - each of them represent a student
                studentsDialog.setSingleChoiceItems(listItemsToShow, -1,
                DialogInterface.OnClickListener {studentsDialog,which->
                    selectedValue=which
                })
                // Set the cancel button click listener
                studentsDialog.setNeutralButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                //Set the positive button click listener
                studentsDialog.setPositiveButton("View") { dialog, _ ->
                    if (selectedValue != -1) {
                        //start the activity related with the student profile - send the student email
                        val intent = Intent(this, StudentProfileActivity::class.java).apply {
                            putExtra(EXTRA_STUDENT_PROFILE, kidsNames[selectedValue])
                        }
                        startActivity(intent)
                        dialog.dismiss()
                    }
                }
                //create the dialog and show it in screen
                val mDialog = studentsDialog.create()
                mDialog.show()
            }
        }
    }
}







