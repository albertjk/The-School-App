package com.example.schoolapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    // This variable is used to access a Cloud Firestore instance
    private val db = Firebase.firestore

    private val TAG = MainActivity::class.qualifiedName

    // Views.
    private lateinit var textInputEmailOrUName : TextInputLayout
    private lateinit var textInputPassword : TextInputLayout
    private lateinit var loginButton : Button
    private lateinit var forgottenPasswordButton : Button
    private lateinit var signUpButton : Button

    val EXTRA_STUDENT_HOME: String = "com.example.schoolapp.EXTRA_STUDENT_HOME"
    val EXTRA_ADMIN_HOME: String = "com.example.schoolapp.EXTRA_ADMIN_HOME"
    val EXTRA_PARENT_HOME: String = "com.example.schoolapp.EXTRA_PARENT_HOME"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textInputEmailOrUName = findViewById(R.id.login_text_input_username_email)
        textInputPassword = findViewById(R.id.text_input_password)
        loginButton = findViewById(R.id.login_button)
        forgottenPasswordButton = findViewById(R.id.forgotten_password_button)
        signUpButton = findViewById(R.id.sign_up_button_activity_main)

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivityPart1::class.java))
        }

        forgottenPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgottenPasswordActivity::class.java))
        }
    }

    /**
     * Checks the entered input and begins the login process.
     */
    fun login(view: View) {

        // Get the user input.
        var emailOrUName: String = textInputEmailOrUName.editText?.text.toString().trim()
        var password: String = textInputPassword.editText?.text.toString().trim()

        // Check if the user entered text.
        var emailOrUNameEntered: Boolean = checkIfFieldIsEmpty(emailOrUName, textInputEmailOrUName)
        var passwordEntered: Boolean = checkIfFieldIsEmpty(password, textInputPassword)

        if (!emailOrUNameEntered || !passwordEntered) {
            Toast.makeText(this, "Please enter your e-mail/username and password", Toast.LENGTH_LONG).show()
        } else {

            /* If the entered text in the e-mail/username field matches the e-mail pattern,
            then it is an e-mail input, so a student is trying to log in, as students have e-mail addresses. */
            if(Patterns.EMAIL_ADDRESS.matcher(emailOrUName).matches()) {
                performLogin(emailOrUName, password, "students", "email")
            }
            // Otherwise, it is a username input, so an admin or a parent is trying to log in, as they have usernames.
            else {

                // First check if the user is an admin. i.e. check the admin collection. Check if the username matches any entry.
                var adminUNameFound = false
                var storedPassword : String? = null

                // Search the admin's username in the 'admin' collection in the DB.
                db.collection("admin")
                    .whereEqualTo("username", emailOrUName)
                    .get()

                    // If the admin username is found, also find the stored password.
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            for (item in document.data) {
                                adminUNameFound = true
                                if (item.key == "password") {
                                    storedPassword = item.value.toString()
                                    Log.d(TAG, "Stored password: " + storedPassword)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "get failed with ", exception)
                    }
                    .addOnCompleteListener {

                            // If the admin username is not present in the DB, the user who is trying to log in is a parent.
                            if (!adminUNameFound) {
                                performLogin(emailOrUName, password, "parents", "username")
                            }
                            // Otherwise, the admin username was found in the DB. Check if the associated password is correct.
                            else {
                                Log.d(TAG, "Entered password: " + password.hashCode().toString())

                                // If the password is incorrect, do not let the admin log in.
                                if (password.hashCode().toString() != storedPassword) {
                                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_LONG)
                                        .show()
                                }
                                // Otherwise, let the admin log in.
                                else {
                                    Toast.makeText(this, "Logged in", Toast.LENGTH_LONG)
                                        .show()

                                    intent = Intent(this, AdminHomeActivity::class.java).apply {
                                        putExtra(EXTRA_ADMIN_HOME, emailOrUName)

                                        // The currentUser value of 0 means the user is the admin.
                                        putExtra("currentUser","0")
                                    }
                                    startActivity(intent)
                                }
                            }
                    }
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

    /**
     * Checks if the e-mail/username and password are valid and match an account and logs in the given student/parent.
     */
    private fun performLogin(emailOrUName: String, password: String, collectionName : String, fieldName : String) {

        var emailOrUNameFound = false
        var storedPassword : String? = null

        // Search the e-mail/username in the DB in the defined collection ('students' or 'parents').
        db.collection(collectionName)
            .whereEqualTo(fieldName, emailOrUName)
            .get()

            // If the e-mail/username is found, also find the stored password.
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    for (item in document.data) {
                        emailOrUNameFound = true
                        if (item.key == "password") {
                            storedPassword = item.value.toString()
                            Log.d(TAG, "Stored password: " + storedPassword)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // If the e-mail/username is not present in the DB, tell the user.
                    if (!emailOrUNameFound) {

                        // Show the message relevant to the type of user.
                        if(collectionName == "students") {
                            Toast.makeText(this, "This e-mail address is not registered", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            Toast.makeText(this, "This username is not registered", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    // Otherwise, the e-mail/username was found in the DB. Check if the associated password is correct.
                    else {
                        Log.d(TAG, "Entered password: " + password.hashCode().toString())

                        // If the password is incorrect, do not let the user log in.
                        if (password.hashCode().toString() != storedPassword) {
                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_LONG)
                                .show()
                        }
                        // Otherwise, let the user log in.
                        else {
                            Toast.makeText(this, "Logged in", Toast.LENGTH_LONG)
                                .show()

                            if(collectionName == "students") {
                                intent = Intent(this, StudentHomeActivity::class.java).apply {
                                    putExtra(EXTRA_STUDENT_HOME, emailOrUName)

                                    // The currentUser value of 1 means the user is not the admin.
                                    putExtra("currentUser","1")
                                }
                            } else {

                                intent = Intent(this, ParentsHomeActivity::class.java).apply {
                                    putExtra(EXTRA_PARENT_HOME, emailOrUName)

                                    // The currentUser value of 1 means the user is not the admin.
                                    putExtra("currentUser","1")
                                }
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
    }
}