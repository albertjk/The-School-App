package com.example.schoolapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.regex.Pattern

class ResetPasswordActivity : AppCompatActivity() {

    // This variable is used to access a Cloud Firestore instance.
    private val db = Firebase.firestore

    private val TAG = ResetPasswordActivity::class.qualifiedName

    // This string wil store the DB collection name ("students" or "parents") to update the user's document in.
    private var collectionName : String? = null

    // This string will store the user's e-mail/username.
    private var emailOrUName : String? = null

    // Views.
    private lateinit var textInputPassword : TextInputLayout
    private lateinit var textInputPassword2 : TextInputLayout

    // The entered password will be matched against this regular expression.
    private val PASSWORD_PATTERN: Pattern =
        Pattern.compile(
            "^" +                     // beginning of the string
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[a-z])" +         // at least 1 lowercase letter
                    "(?=.*[A-Z])" +         // at least 1 uppercase letter
                    "(?=.*[@#$%^&+=])" +    // at least 1 special character
                    "(?=\\S+$)" +           // no whitespace
                    ".{5,}" +               // at least 5 characters long
                    "$"                     // end of the string
        );

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        textInputPassword = findViewById(R.id.reset_password_text_input_password1)
        textInputPassword2 = findViewById(R.id.reset_password_text_input_password2)

        // From the intent, get the collection name, which identifies the type of user (student or parent).
        collectionName = intent.getStringExtra(ForgottenPasswordActivity().EXTRA_COLLECTION_NAME)

        // Also get the e-mail or username.
        emailOrUName = intent.getStringExtra(ForgottenPasswordActivity().EXTRA_EMAIL_OR_USERNAME)
    }

    /**
     * Click handler function, which performs the password reset.
     */
    fun resetPassword(view: View) {

        // Get the user input.
        var password1: String = textInputPassword.editText?.text.toString().trim()
        var password2: String = textInputPassword2.editText?.text.toString().trim()

        // Validate the entered password.
        var validPassword: Boolean = validatePassword(password1)
        var passwordsMatch: Boolean = false

        if (password2 != password1) {
            textInputPassword2.error = "The passwords must match."
        } else {
            textInputPassword2.error = null
            passwordsMatch = true
        }

        // If the input was not valid, let the user know.
        if (!validPassword || !passwordsMatch) {
            Toast.makeText(this, "Please fix the input errors", Toast.LENGTH_LONG).show()
        }
        // Otherwise, update the user's document in the DB.
        else {

            /* Find the user's document in the defined collection ('students' or 'parents')
            by their e-mail (if student) or username (if parent).
            Update the user's password by hashing the new password.
            Then, redirect the user to MainActivity.*/
            collectionName?.let {
                emailOrUName?.let { it1 ->
                    db.collection(it).document(it1)
                        .update("password", password1.hashCode())
                        .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully updated!") }
                        .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
                        .addOnCompleteListener {
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                }
            }
        }
    }

    /**
     * Validates the entered password.
     */
    private fun validatePassword(password: String): Boolean {

        // If the password field is empty, show an error message.
        if (password.isEmpty()) {
            textInputPassword.error = "Field cannot be empty"
            return false
        }
        // If the password is too weak, show an error message.
        else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            textInputPassword.error =
                "The password must contain at least 1 lowercase and 1 uppercase letter, 1 digit, 1 special character, and must be at least 5 characters long"
            return false
        } else {
            textInputPassword.error = null
            return true
        }
    }
}
