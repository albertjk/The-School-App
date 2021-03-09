package com.example.schoolapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ForgottenPasswordActivity : AppCompatActivity() {

    // This variable is used to access a Cloud Firestore instance
    private val db = Firebase.firestore

    private val TAG = ForgottenPasswordActivity::class.qualifiedName

    private lateinit var textInputEmailOrUName : TextInputLayout
    private lateinit var textInputCode : TextInputLayout
    private lateinit var submitButton : Button

    // This string wil store the DB collection name ("students" or "parents") to search the entered input in.
    private var collectionName : String? = null

    // This string will store the entered e-mail/username.
    private var emailOrUName : String? = null

    // This boolean is false initially, but if the user gives SMS-sending permission to the app, it becomes true.
    private var allowSMS : Boolean = false

    private val SMS_REQUEST_CODE : Int = 123

    var EXTRA_COLLECTION_NAME: String = "com.example.schoolapp.EXTRA_COLLECTION_NAME"
    var EXTRA_EMAIL_OR_USERNAME: String = "com.example.schoolapp.EXTRA_EMAIL_OR_USERNAME"

    /* This global variable will store the generated PIN code. This will be sent to the user in an
    SMS message, and then will be checked against what they enter. */
    var generatedCode : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgotten_password)

        textInputEmailOrUName = findViewById(R.id.forgotten_password_text_input_username_email)
        textInputCode = findViewById(R.id.forgotten_password_text_input_pin_code)
        submitButton = findViewById(R.id.submit_button)
    }

    /**
     * Click handler function for the Find Account button. Checks the user input and calls the sendPIN function.
     */
    fun sendCode(view: View) {

        // Get the user input.
        emailOrUName = textInputEmailOrUName.editText?.text.toString().trim()

        // Check if the field is empty.
        var emailOrUNameEntered: Boolean = checkIfFieldIsEmpty(emailOrUName!!, textInputEmailOrUName)

        if (!emailOrUNameEntered) {
            Toast.makeText(this, "Please enter your e-mail or username", Toast.LENGTH_LONG).show()
        } else {

            /* If the entered text in the e-mail/username field matches the e-mail pattern,
            then it is an e-mail input, so send a PIN to the user who is a student. */
            if(Patterns.EMAIL_ADDRESS.matcher(emailOrUName).matches()) {
                collectionName = "students"
                sendPIN("email")
            }
            // Otherwise, it is a username input, so send a PIN to the user who is a parent.
            else {
                collectionName = "parents"
                sendPIN("username")
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
     * Checks if the e-mail/username is valid and matches an account and sends a PIN to the user.
     */
    private fun sendPIN(fieldName : String) {

        var emailOrUNameFound = false
        var phoneNum : String? = null

        // Search the e-mail/username in the DB in the defined collection ('students' or 'parents').
        collectionName?.let {
            db.collection(it)
                .whereEqualTo(fieldName, emailOrUName)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        for (item in document.data) {
                            emailOrUNameFound = true
                            Log.d(TAG, "Email/Username is found")

                            // If the user is a parent, find their phone number.
                            if(item.key == "phone") {
                                phoneNum = item.value.toString()
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
                                Toast.makeText(this, "This student e-mail address is not registered", Toast.LENGTH_LONG)
                                    .show()
                            } else {
                                Toast.makeText(this, "This parent username is not registered", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                        // Otherwise, the e-mail/username was found.
                        else {
                            Toast.makeText(this, "PIN code is sent", Toast.LENGTH_LONG)
                                .show()

                            generatedCode = generatePIN()

                            /* Once the user has given permission to the app to send text messages,
                            send the generated PIN code in an SMS. */
                            checkSMSPermission()
                            if(allowSMS) {
                                phoneNum?.let { it1 -> sendSMS(it1) }

                                // Allow the user to enter the code and submit it.
                                textInputCode.isEnabled = true
                                submitButton.isEnabled = true
                            }
                        }
                    }
                }
        }
    }

    /**
     * Generates a random 5-digit PIN code.
     */
    private fun generatePIN(): String {
        val STRING_LENGTH = 5;
        val charPool: List<Char> = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val pin = (1..STRING_LENGTH)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("");

        return pin
    }

    /**
     * Checks if the user has given permission to the application to send SMS messages.
     */
    private fun checkSMSPermission() {

        // If the permission is not granted, request it.
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_REQUEST_CODE)
        }
        // Otherwise, the permission is already granted.
        else {
            allowSMS = true
        }
    }

    /**
     * Callback for the result from requesting permission to send SMS messages.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == SMS_REQUEST_CODE && permissions[0].equals(Manifest.permission.SEND_SMS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            allowSMS = true
        }
    }

    /**
     * Sends the generated PIN code to the given user's phone number.
     */
    private fun sendSMS(phoneNum : String) {
        val message = "Dear " + emailOrUName + ",\nYour PIN code is " + generatedCode + "\nSent from the School App"
        SmsManager.getDefault().sendTextMessage(phoneNum, null, message, null, null)
    }

    /**
     * Click handler function for the Submit button. Checks the user input and redirects to ResetPasswordActivity.
     */
    fun submitEnteredCode(view: View) {

        // Get the user input.
        var enteredCode: String = textInputCode.editText?.text.toString().trim()

        // Check if the field is empty.
        var isCodeEntered: Boolean = checkIfFieldIsEmpty(enteredCode, textInputCode)

        if (!isCodeEntered) {
            Toast.makeText(this, "Please enter the PIN code you received to your phone number", Toast.LENGTH_LONG).show()
        }
        // If the PIN code is entered, check if it matches the one that was generated.
        else {

            // If the codes match, redirect the user to ResetPasswordActivity.
            if (enteredCode == generatedCode) {
                Toast.makeText(this, "The code is correct", Toast.LENGTH_LONG).show()

                // Pass the collectionName ('students' or 'parents') and the e-mail/username as intent extras.
                intent = Intent(this, ResetPasswordActivity::class.java).apply {
                    putExtra(EXTRA_COLLECTION_NAME, collectionName)
                    putExtra(EXTRA_EMAIL_OR_USERNAME, emailOrUName)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "The code is incorrect", Toast.LENGTH_LONG).show()
            }
        }
    }
}
