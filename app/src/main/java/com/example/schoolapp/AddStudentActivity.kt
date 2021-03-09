package com.example.schoolapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddStudentActivity : AppCompatActivity() {

    // This variable is used to access a Cloud Firestore instance.
    private val db = Firebase.firestore

    private val TAG = AddStudentActivity::class.qualifiedName

    /* This list will store the e-mails of all students in the 'all_student_emails' collection
    whether they are registered or not. */
    private var studentEmails: ArrayList<String>? = null

    // Views.
    private lateinit var textInputFirstName : TextInputLayout
    private lateinit var textInputLastName : TextInputLayout
    private lateinit var textInputClassId : TextInputLayout
    private lateinit var textInputEmail : TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        textInputFirstName = findViewById(R.id.add_a_student_text_input_student_first_name)
        textInputLastName = findViewById(R.id.add_a_student_text_input_student_last_name)
        textInputClassId = findViewById(R.id.add_a_student_text_input_student_class)
        textInputEmail = findViewById(R.id.add_a_student_text_input_student_email)

        /* Query all student e-mails from the 'all_student_emails' collection in the DB. Store them in the
        studentEmails array list. The list will be used to check if the entered e-mail is in the collection. */
        getAllStudentEmails()
    }

    /**
     * Queries the database to get all student e-mail addresses and stores them in the studentEmails list.
     */
    private fun getAllStudentEmails() {
        studentEmails = ArrayList()

        db.collection("all_student_emails")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "DOC ID => DOC DATA")
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                    studentEmails!!.add(document.id)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Click handler method which adds the new student to the 'all_student_emails' collection in the DB.
     */
    fun addStudent(view: View) {

            // Get the user input.
            var firstName : String = textInputFirstName.editText?.text.toString().trim()
            var lastName : String = textInputLastName.editText?.text.toString().trim()
            var classId : String = textInputClassId.editText?.text.toString().trim()
            var email: String = textInputEmail.editText?.text.toString().trim()

            // Check if the fields are empty and validate the input.
            var firstNameEntered: Boolean = checkIfFieldIsEmpty(firstName!!, textInputFirstName)
            var lastNameEntered: Boolean = checkIfFieldIsEmpty(lastName!!, textInputLastName)
            var classIdEntered: Boolean = checkIfFieldIsEmpty(classId!!, textInputClassId)
            var validEmail: Boolean = validateEmail(email)

            if(!firstNameEntered || !lastNameEntered || !classIdEntered || !validEmail) {
                Toast.makeText(this, "Please enter valid input", Toast.LENGTH_LONG).show()
            }
            else {

                // Add the student to the 'all_student_emails' collection in the database.
                val student = hashMapOf(
                    "first name" to firstName,
                    "last Name" to lastName,
                    "class" to classId,
                    "email" to email,
                    "registered" to "false" // Initially the student is not registered. They have to sign up themselves.
                )
                db.collection("all_student_emails").document(email)
                    .set(student)
                    .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
                    .addOnCompleteListener (this) {
                        Toast.makeText(this, "Student is added", Toast.LENGTH_SHORT).show()

                        // Clear all input to let the administrator add another student if they wish.
                        textInputFirstName.editText?.text?.clear()
                        textInputLastName.editText?.text?.clear()
                        textInputClassId.editText?.text?.clear()
                        textInputEmail.editText?.text?.clear()
                    }
            }
    }

    /**
     * Checks if the given field is empty and shows an error message if it is.
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

    /**
     * Validates the entered e-mail address.
     */
    private fun validateEmail(email: String): Boolean {

        // If the e-mail field is empty, show a message.
        if (email.isEmpty()) {
            textInputEmail.error = "Field cannot be empty"
            return false
        }
        // If the e-mail does not match the pattern of an e-mail address, show a message.
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputEmail.error = "Please enter a valid e-mail address"
            return false
        }
        /* Check if a student with the given e-mail is already in the 'all_student_emails' collection.
        This means, there is no need to add the student again. */
        else if(studentEmails?.contains(email)!!) {
            textInputEmail.error = "This e-mail address is already in the 'all_student_emails' collection"
            return false
        }
        // Otherwise, the e-mail is not yet added, so it is valid.
        else {
            textInputEmail.error = null
            return true
        }
    }
}