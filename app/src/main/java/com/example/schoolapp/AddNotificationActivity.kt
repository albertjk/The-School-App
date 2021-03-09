package com.example.schoolapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddNotificationActivity : AppCompatActivity() {

    // This variable is used to access a Cloud Firestore instance.
    private val db = Firebase.firestore

    private val TAG = AddNotificationActivity::class.qualifiedName

    // Views.
    private lateinit var textInputNotificationTitle : TextInputLayout
    private lateinit var textInputNotificationContent : TextInputLayout
    private val currentTimestamp = Timestamp.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_notification)

        textInputNotificationTitle = findViewById(R.id.add_input_notification_title)
        textInputNotificationContent = findViewById(R.id.add_notification_content)
    }

    /**
     * Adds a notification to the DB.
     */
    fun addNotification(view: View) {

        // Get the user input.
        var title : String = textInputNotificationTitle.editText?.text.toString().trim()
        var content : String = textInputNotificationContent.editText?.text.toString().trim()

        // Check if the fields are empty and validate the input.
        var filledTitle: Boolean = checkIfFieldIsEmpty(title!!, textInputNotificationTitle)
        var filledContent: Boolean = checkIfFieldIsEmpty(content!!, textInputNotificationContent)

        if(!filledTitle || !filledContent) {
            Toast.makeText(this, "Please enter valid input", Toast.LENGTH_LONG).show()
        }
        else {

            // Add the notification to the 'notifications' collection in the database.
            val notification = hashMapOf(
                "title" to title,
                "content" to content,
                "date" to currentTimestamp
            )
            db.collection("notifications").document(title)
                .set(notification)
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
                .addOnCompleteListener (this) {
                    Toast.makeText(this, "Notification is added", Toast.LENGTH_SHORT).show()

                    // Clear all input and let the administrator add another notification if they wish.
                    textInputNotificationTitle.editText?.text?.clear()
                    textInputNotificationContent.editText?.text?.clear()
                }
        }
    }

    /**
     * Checks if the given field is empty or not and shows an error message if it is.
     */
    private fun checkIfFieldIsEmpty(text: String, textInputLayout: TextInputLayout): Boolean {
        return if (text.isEmpty()) {
            textInputLayout.error = "Field cannot be empty"
            false
        } else {
            textInputLayout.error = null
            true
        }
    }
}