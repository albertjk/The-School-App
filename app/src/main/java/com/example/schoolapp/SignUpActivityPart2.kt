package com.example.schoolapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.regex.Pattern

class SignUpActivityPart2 : AppCompatActivity() {

    // This variable is used to access a Cloud Firestore instance.
    private val db = Firebase.firestore

    private val TAG = SignUpActivityPart2::class.qualifiedName

    /* All student e-mail addresses and whether they are registered will be saved into this HashMap
    after querying the DB. Key: e-mail address (which is unique). Value: registered or not. */
    private var studentEmails: HashMap<String, Boolean>? = null

    // Views needed for student sign up and some others also needed for parent sign up.
    private lateinit var textInputEmail : TextInputLayout
    private lateinit var textInputPassword : TextInputLayout
    private lateinit var textInputPassword2 : TextInputLayout
    private lateinit var textInputStudentPhone : TextInputLayout
    private lateinit var textInputParent1Phone : TextInputLayout
    private lateinit var textInputParent2Phone : TextInputLayout

    // These additional views are needed for the parent sign up process.
    private lateinit var textInputParentFirstName : TextInputLayout
    private lateinit var textInputParentLastName : TextInputLayout
    private lateinit var textInputReceivedUsername : TextInputLayout

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

    private var parent1Phone : String? = null
    private var parent2Phone : String? = null

    // This boolean is false initially, but if the user gives SMS-sending permission to the app, it becomes true.
    private var allowSMS : Boolean = false

    private val SMS_REQUEST_CODE : Int = 123

    /* All parent usernames from the DB will be saved into this array list before the parent signs up.
    It will be checked if the entered username matches any of the usernames in the list. */
    private var parentUsernames: ArrayList<String>? = null

    // This variable will store the e-mail address of the student who is added to the DB after signing up.
    private var emailOfAddedKid : String? = null

    /* These variables will store the generated usernames of the two parents.
    They need to be accessed in multiple functions. */
    private var parent1GeneratedUsername : String? = null
    private var parent2GeneratedUsername : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the sign up selection string ("Student" or "Parent") from the Intent.
        val signUpSelection: String =
            intent.getStringExtra(SignUpActivityPart1().EXTRA_SIGN_UP_SELECTION)

        // Set the layout depending on the radio button selection in SignUpActivityPart1 (student or parent sign up).
        if (signUpSelection == "Student") {
            setContentView(R.layout.activity_sign_up_part2_student)

            textInputEmail = findViewById(R.id.student_text_input_email)
            textInputPassword = findViewById(R.id.student_text_input_password1)
            textInputPassword2 = findViewById(R.id.student_text_input_password2)
            textInputStudentPhone = findViewById(R.id.student_text_input_student_phone)
            textInputParent1Phone = findViewById(R.id.student_text_input_parent1_phone)
            textInputParent2Phone = findViewById(R.id.student_text_input_parent2_phone)

            /* Query all student e-mails from the DB once only. Store them in the studentEmails HashMap.
            This HashMap will be used to check if the entered e-mail matches a student e-mail and
            if this e-mail is already registered or not. */
            getAllStudentEmails()

            // Generate two random usernames for the student's parents.
            parent1GeneratedUsername = generateParentUsername()
            parent2GeneratedUsername = generateParentUsername()
        } else {
            setContentView(R.layout.activity_sign_up_part2_parent)

            textInputParentFirstName = findViewById(R.id.parent_text_input_first_name)
            textInputParentLastName = findViewById(R.id.parent_text_input_last_name)
            textInputReceivedUsername = findViewById(R.id.parent_text_input_username)
            textInputPassword = findViewById(R.id.parent_text_input_password1)
            textInputPassword2 = findViewById(R.id.parent_text_input_password2)

            /* Query all parent usernames from the DB once only and store them in the parentUsernames array list.
            It will be checked if the entered username matches any of the usernames stored in this list. */
            getAllParentUsernames()
        }
    }

    /**
     * Queries the database once to find all existing parent usernames.
     */
    private fun getAllParentUsernames() {
        parentUsernames = ArrayList()

        db.collection("parents")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "DOC ID => DOC DATA")
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                    parentUsernames!!.add(document.id)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Queries the database to find all valid student e-mail addresses and whether they are registered or not.
     */
    private fun getAllStudentEmails() {
        studentEmails = HashMap()

        var extractedEmail: String? = null
        var registered: Boolean? = null

        /* Get all student e-mail addresses which are in the database. Only these are valid student e-mails.
        Also get if they are registered or not. */
        db.collection("all_student_emails")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    for (item in document.data) {
                        Log.d(TAG, "item.key: "+ item.key)
                        if (item.key == "email") {
                            extractedEmail = item.value as String
                        } else if (item.key == "registered") {
                            registered = item.value as Boolean
                        }
                        if (extractedEmail != null && registered != null) {
                            studentEmails!!.put(extractedEmail!!, registered!!)
                            extractedEmail = null
                            registered = null
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Generates a random string of length 5 of lowercase and uppercase letters and digits.
     * This string will become the username of the given parent.
     */
    private fun generateParentUsername() : String {
        val STRING_LENGTH = 5;
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val username = (1..STRING_LENGTH)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("");

        return username
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
        // If the entered e-mail does not match the pattern of an e-mail address, show a message.
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputEmail.error = "Please enter a valid e-mail address"
            return false
        }
        /* Check if the student e-mail address is in the studentEmails HashMap.
        This means that the e-mail address is a recognised student-email. */
        else if (studentEmails?.containsKey(email)!!) {
            // Check if it is registered (i.e. already used by a registered student).
            if (studentEmails!![email] == true) {
                textInputEmail.error = "This e-mail address is already used by a student"
                return false
            }
            // If the e-mail is not yet registered, it can be used by the student signing up at the moment.
            else {
                textInputEmail.error = null
                return true
            }
        }
        // If the e-mail address was not in the HashMap, then it is not a recognised and valid student e-mail address.
        else {
            textInputEmail.error = "This is not a recognised student e-mail address"
            return false
        }
    }

    /**
     * Validates the entered password.
     */
    private fun validatePassword(password: String): Boolean {

        // If the first password field is empty, show an error message.
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

    /**
     * Performs simple validation of the phone number fields.
     */
    private fun validatePhone(phoneNum: String, textInputPhone: TextInputLayout): Boolean {

        // If the given phone number field is empty, show an error message.
        if (phoneNum.isEmpty()) {
            textInputPhone.error = "Field cannot be empty"
            return false
        } else {
            textInputPhone.error = null
            return true
        }
    }

    /**
     * Validates the entered username.
     */
    private fun validateUsername(username: String): Boolean {

        // If the username field is empty, show a message.
        if (username.isEmpty()) {
            textInputReceivedUsername.error = "Field cannot be empty"
            return false
        }
        /* Check if the entered username is in the parentUsernames array list.
        If it is not in this list, it means it is not a recognised username, so it cannot be used for registration. */
        else if (!parentUsernames?.contains(username)!!) {
            textInputReceivedUsername.error = "This username is not recognised"
            return false
        }
        // Otherwise, the username is recognised, so it is valid.
        else {
            textInputReceivedUsername.error = null
            return true
        }
    }

    /**
     * Validates the entered name.
     */
    private fun validateName(name: String, textInput: TextInputLayout): Boolean {

        // If the field is empty, show an error message.
        if (name.isEmpty()) {
            textInput.error = "Field cannot be empty"
            return false
        } else {
            textInput.error = null
            return true
        }
    }

    /**
     * This method is called when the user taps the Sign Up button on the Student Sign Up screen.
     */
    fun signUpStudent(view: View) {

        // Get the user input.
        var email: String = textInputEmail.editText?.text.toString().trim()
        var password1: String = textInputPassword.editText?.text.toString().trim()
        var password2: String = textInputPassword2.editText?.text.toString().trim()
        var studentPhone : String = textInputStudentPhone.editText?.text.toString().trim()
        parent1Phone = textInputParent1Phone.editText?.text.toString().trim()
        parent2Phone = textInputParent2Phone.editText?.text.toString().trim()

        // Validate the input.
        var validEmail: Boolean = validateEmail(email)
        var validPassword: Boolean = validatePassword(password1)
        var passwordsMatch : Boolean = false

        if (password2 != password1) {
            textInputPassword2.error = "The passwords must match."
        } else {
            textInputPassword2.error = null
            passwordsMatch = true
        }

        var validStudentPhone : Boolean = validatePhone(studentPhone!!, textInputStudentPhone)
        var validPhone1: Boolean = validatePhone(parent1Phone!!, textInputParent1Phone)
        var validPhone2: Boolean = validatePhone(parent2Phone!!, textInputParent2Phone)

        Log.d(
            TAG,
            "validEmail: " + validEmail + " validPassword: " + validPassword + " passwordsMatch: " + passwordsMatch + " validStudentPhone: " + validStudentPhone + " validPhone1: " + validPhone1 + " validPhone2: " + validPhone2)

        // If everything was valid, register the student.
        if (validEmail && validPassword && passwordsMatch && validStudentPhone && validPhone1 && validPhone2) {
            emailOfAddedKid = email

            /* Get the name and class ID of the student who is being registered.
            Add the student to the 'students' collection in the database. */
            var firstName: String? = null
            var lastName: String? = null
            var classId: String? = null
            db.collection("all_student_emails")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {

                        // Iterate over each data item about the student.
                        for (item in document.data) {
                            Log.d(TAG, "item: " + item)
                            if (item.key == "first name") {
                                firstName = item.value as String
                                Log.d(TAG, "item.value: " + item.value)
                            }
                            else if (item.key == "last name") {
                                lastName = item.value as String
                                Log.d(TAG, "item.value: " + item.value)
                            }
                            else if (item.key == "class") {
                                classId = item.value as String
                                Log.d(TAG, "item.value: " + item.value)
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        classId?.let {
                            firstName?.let { it1 ->
                                lastName?.let { it2 ->
                                    studentPhone?.let { it3 ->
                                        parent1Phone?.let { it4 ->
                                            parent2Phone?.let { it5 ->
                                                addStudentToDB(
                                                    it,
                                                    email,
                                                    it1,
                                                    it2,
                                                    it3,
                                                    it4,
                                                    it5,
                                                    password1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Please fix the input errors", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Adds a given student to the 'students' collection in the database.
     * The password is hashed before it is stored in the database.
     */
    private fun addStudentToDB(classId: String, email: String, firstName: String, lastName: String, studentPhone : String, parent1Phone: String, parent2Phone: String, password1: String) {

        val student = hashMapOf(
            "class" to classId,
            "email" to email,
            "phone" to studentPhone,
            "first name" to firstName,
            "last Name" to lastName,
            "parent1 phone" to parent1Phone,
            "parent2 phone" to parent2Phone,
            "password" to password1.hashCode()
        )
        db.collection("students").document(email)
            .set(student)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
            .addOnCompleteListener (this) {

                /* After registering the student, set the 'registered' field to true in the
                student's document in the 'all_student_emails' collection . */
                db.collection("all_student_emails").document(email)
                    .update("registered", true)
                    .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully updated!") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
                    .addOnCompleteListener(this) {task ->
                        if (task.isSuccessful) {

                            /* Once the user has given permission to the app to send text messages,
                            check if the parent phone numbers are already in the system
                            and send the usernames to the parents in SMS text messages. */
                            checkSMSPermission()
                            if(allowSMS) {
                                checkParentsAreAlreadyStored(parent1Phone, parent2Phone)
                            }
                        }
                    }
            }
    }

    /**
     * Checks if the parents are already stored in the DB, and if so updates their records by
     * adding the new student to their list of kids.
     * If one or two of the parents are not stored, then they are added to the DB.
     */
    private fun checkParentsAreAlreadyStored(parent1Phone: String, parent2Phone: String) {
        var parent1AlreadyStored = false
        var parent2AlreadyStored = false

        // Search parent 1 by phone number. If their document exists in the DB, do not send a username to them.
        db.collection("parents")
            .whereEqualTo("phone", parent1Phone)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    for (item in document.data) {
                        Log.d(TAG, "item: " + item)
                        if (item.value == parent1Phone) {
                            parent1AlreadyStored = true
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Search parent 2 by phone number. If their document exists in the DB, do not send a username to them.
                    db.collection("parents")
                        .whereEqualTo("phone", parent2Phone)
                        .get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                for (item in document.data) {
                                    Log.d(TAG, "item: " + item)
                                    if (item.value == parent2Phone) {
                                        parent2AlreadyStored = true
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "get failed with ", exception)
                        }
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                // If both parents are already stored, update the kids records of both parents in the DB.
                                if(parent1AlreadyStored && parent2AlreadyStored) {

                                    var p1Username : String? = null
                                    var p2Username : String? = null

                                    var kids : ArrayList<String>? = null

                                    // Find parent1 in the DB to add all their kids' e-mails already stored in the DB.
                                    db.collection("parents")
                                        .whereEqualTo("phone", parent1Phone)
                                        .get()
                                        .addOnSuccessListener { documents ->

                                            // If the parent is found, get the registered kids' e-mails.
                                            for (document in documents) {

                                                // Iterate over each data item about the parent.
                                                for (item in document.data) {
                                                    Log.d(TAG, "item: " + item)

                                                    // Get the list of kid e-mails.
                                                    if (item.key == "kids") {
                                                        kids =
                                                            document["kids"] as ArrayList<String>?
                                                    }
                                                    if(item.key == "username") {
                                                        p1Username = item.value as String?
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {

                                            // Add the new kid's email.
                                            emailOfAddedKid?.let { it1 -> kids?.add(it1) }

                                            // Update the parent record with the all the kids' e-mails stored.
                                            db.collection("parents").document(
                                                p1Username!!
                                            )
                                                .update("kids", kids)
                                                .addOnSuccessListener {
                                                    Log.d(
                                                        TAG,
                                                        "DocumentSnapshot successfully updated!"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w(
                                                        TAG,
                                                        "Error updating document",
                                                        e
                                                    )
                                                }
                                                .addOnCompleteListener {

                                                    var kids2 : ArrayList<String>? = null

                                                    // Find parent2 in the DB to add all their kids' e-mails already stored in the DB.
                                                    db.collection("parents")
                                                        .whereEqualTo("phone", parent2Phone)
                                                        .get()
                                                        .addOnSuccessListener { documents ->

                                                            // If the parent is found, get the registered kids' e-mails.
                                                            for (document in documents) {

                                                                // Iterate over each data item about the parent.
                                                                for (item in document.data) {
                                                                    Log.d(TAG, "item: " + item)

                                                                    // Get the list of kid e-mails.
                                                                    if (item.key == "kids") {
                                                                        kids2 =
                                                                            document["kids"] as ArrayList<String>?
                                                                    }
                                                                    if(item.key == "username") {
                                                                        p2Username = item.value as String?
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .addOnCompleteListener {

                                                            // Add the new kid's email.
                                                            emailOfAddedKid?.let { it1 -> kids2?.add(it1) }

                                                            // Update the parent record with the all the kids' e-mails stored.
                                                            db.collection("parents").document(
                                                                p2Username!!
                                                            )
                                                                .update("kids", kids2)
                                                                .addOnSuccessListener {
                                                                    Log.d(
                                                                        TAG,
                                                                        "DocumentSnapshot successfully updated!"
                                                                    )
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    Log.w(
                                                                        TAG,
                                                                        "Error updating document",
                                                                        e
                                                                    )
                                                                }
                                                                .addOnCompleteListener  (this) { task ->

                                                                    // After finishing, redirect to StudentHome.
                                                                    if (task.isSuccessful) {

                                                                        Log.d(TAG, "Successful Sign up")

                                                                        Toast.makeText(this, "Sign up is successful", Toast.LENGTH_SHORT)
                                                                            .show()
                                                                        startActivity(Intent(this, StudentHomeActivity::class.java))
                                                                    }
                                                                }
                                                        }
                                                }
                                        }
                                }
                                // If only parent1 is already stored, update their record, and add parent2 to the DB.
                                else if(parent1AlreadyStored && !parent2AlreadyStored) {

                                    var kids: ArrayList<String>? = null
                                    var p1Username : String? = null

                                    // Find parent1 in the DB based on their phone number to get all their kids' e-mails already stored.
                                    db.collection("parents")
                                        .whereEqualTo("phone", parent1Phone)
                                        .get()
                                        .addOnSuccessListener { documents ->

                                            // If the parent is found, get the registered kids' e-mails.
                                            for (document in documents) {

                                                // Iterate over each data item about the parent.
                                                for (item in document.data) {
                                                    Log.d(TAG, "item: " + item)

                                                    // Get the list of kid e-mails.
                                                    if (item.key == "kids") {
                                                        kids =
                                                            document["kids"] as ArrayList<String>?
                                                    }
                                                    if(item.key == "username") {
                                                        p1Username = item.value as String?
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {

                                            // Add the new kid's email.
                                            emailOfAddedKid?.let { it1 -> kids?.add(it1) }

                                            // Update the parent record with the all the kids' e-mails stored.
                                            db.collection("parents").document(
                                                p1Username!!
                                            )
                                                .update("kids", kids)
                                                .addOnSuccessListener {
                                                    Log.d(
                                                        TAG,
                                                        "DocumentSnapshot successfully updated!"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w(
                                                        TAG,
                                                        "Error updating document",
                                                        e
                                                    )
                                                }
                                                .addOnCompleteListener {

                                                    /* Send parent2 an SMS containing their username.
                                                    Then, add parent2 to the DB. */
                                                    parent2GeneratedUsername?.let { it1 ->
                                                        sendSMS(parent2Phone, it1)
                                                        addParentDataToDB(
                                                            it1, parent2Phone)
                                                    }
                                                }
                                        }
                                }
                                // If only parent2 is already stored, update their record, and add parent1 to the DB.
                                else if(!parent1AlreadyStored && parent2AlreadyStored) {

                                    var kids: ArrayList<String>? = null
                                    var p2Username : String? = null

                                    // Find parent2 in the DB to get all their kids' e-mails already stored.
                                    db.collection("parents")
                                        .whereEqualTo("phone", parent2Phone)
                                        .get()
                                        .addOnSuccessListener { documents ->

                                            // If the parent is found, get the registered kids' e-mails.
                                            for (document in documents) {

                                                // Iterate over each data item about the parent.
                                                for (item in document.data) {
                                                    Log.d(TAG, "item: " + item)

                                                    // Get the list of kid e-mails.
                                                    if (item.key == "kids") {
                                                        kids =
                                                            document["kids"] as ArrayList<String>?
                                                    }
                                                    if(item.key == "username") {
                                                        p2Username = item.value as String?
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {

                                            // Add the new kid's email.
                                            emailOfAddedKid?.let { it1 -> kids?.add(it1) }

                                            // Update the parent record with the all the kids' e-mails stored.
                                            db.collection("parents").document(
                                                p2Username!!
                                            )
                                                .update("kids", kids)
                                                .addOnSuccessListener {
                                                    Log.d(
                                                        TAG,
                                                        "DocumentSnapshot successfully updated!"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w(
                                                        TAG,
                                                        "Error updating document",
                                                        e
                                                    )
                                                }
                                                .addOnCompleteListener {

                                                    /* Send parent1 an SMS containing their username.
                                                    Then, add parent1 to the DB. */
                                                    parent1GeneratedUsername?.let { it1 ->
                                                        sendSMS(parent1Phone, it1)
                                                        addParentDataToDB(
                                                            it1, parent1Phone)
                                                    }
                                                }
                                        }
                                }
                                // If neither parents are stored, add both parents to the DB.
                                else {
                                    /* The student currently signing up is the parents' first kid
                                    registered in the school, so add their ID in an array list to the parents' records. */
                                    var kids : ArrayList<String> = ArrayList()
                                    emailOfAddedKid?.let { kids.add(it) }

                                    // Add the parents to the 'parents' collection in the database.
                                    val parent1 = hashMapOf(
                                        "username" to parent1GeneratedUsername,
                                        "phone" to parent1Phone,
                                        "first name" to "",
                                        "last Name" to "",
                                        "kids" to kids,
                                        "password" to ""
                                    )

                                    val parent2 = hashMapOf(
                                        "username" to parent2GeneratedUsername,
                                        "phone" to parent2Phone,
                                        "first name" to "",
                                        "last Name" to "",
                                        "kids" to kids,
                                        "password" to ""
                                    )

                                    db.runBatch {batch ->
                                        parent1GeneratedUsername?.let {
                                            db.collection("parents").document(
                                                it
                                            )
                                        }?.let { batch.set(it, parent1) }
                                        parent2GeneratedUsername?.let {
                                            db.collection("parents").document(
                                                it
                                            )
                                        }?.let { batch.set(it, parent2) }
                                    }
                                        .addOnSuccessListener { Log.d(TAG, "DocumentSnapshots successfully written!") }
                                        .addOnFailureListener { e -> Log.w(TAG, "Error writing documents", e) }
                                        .addOnCompleteListener (this) { task ->

                                            // After finishing, redirect to StudentHome.
                                            if (task.isSuccessful) {
                                                Log.d(TAG, "SUCCESS")

                                                Toast.makeText(this, "Sign up is successful", Toast.LENGTH_SHORT)
                                                    .show()

                                                startActivity(Intent(this, StudentHomeActivity::class.java))
                                            }
                                        }
                                }
                            }
                        }
                }
            }
    }

    /**
     * Adds a given parent to the database.
     */
    private fun addParentDataToDB(username : String, phoneNum : String) {

        // This is the parent's first registered kid, so add their ID in an array list to the parent's record.
        var kids : ArrayList<String> = ArrayList()
        emailOfAddedKid?.let { kids.add(it) }

        // Add the parent to the 'parents' collection in the database.
        val parent = hashMapOf(
            "username" to username,
            "phone" to phoneNum,
            "first name" to "",
            "last name" to "",
            "kids" to kids,
            "password" to ""
        )
        db.collection("parents").document(username)
            .set(parent)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
            .addOnCompleteListener (this) { task ->

                // After finishing, redirect to StudentHome.
                if (task.isSuccessful) {

                    Log.d(TAG, "SUCCESS")

                    Toast.makeText(this, "Sign up is successful", Toast.LENGTH_SHORT)
                        .show()

                    startActivity(Intent(this, StudentHomeActivity::class.java))
                }
            }
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
     * Sends the generated username to the given parent's phone number.
     */
    private fun sendSMS(phoneNum : String, username : String) {
        val message = "Hello!\nYour SchoolApp username is: " + username  + "\nSent from the School App"
        SmsManager.getDefault().sendTextMessage(phoneNum, null, message, null, null)
    }

    /**
     * This method is called when the user taps the Sign Up button on the Parent Sign Up screen.
     */
    fun signUpParent(view: View) {

        // Get the user input.
        var firstName: String = textInputParentFirstName.editText?.text.toString().trim()
        var lastName: String = textInputParentLastName.editText?.text.toString().trim()
        var username : String = textInputReceivedUsername.editText?.text.toString().trim()
        var password1: String = textInputPassword.editText?.text.toString().trim()
        var password2: String = textInputPassword2.editText?.text.toString().trim()

        // Validate the input.
        var validFirstName = validateName(firstName, textInputParentFirstName)
        var validLastName = validateName(lastName, textInputParentLastName)
        var validAndRecognisedUsername = validateUsername(username)
        var validPassword: Boolean = validatePassword(password1)
        var passwordsMatch: Boolean = false

        if (password2 != password1) {
            textInputPassword2.error = "The passwords must match."
        } else {
            textInputPassword2.error = null
            passwordsMatch = true
        }

        // If everything was valid, register the parent.
        if (validFirstName && validLastName && validAndRecognisedUsername && validPassword && passwordsMatch) {

            // Update the parent's document in the 'parents' collection in the database. Store the password's hash.
            db.collection("parents").document(username)
                .update("first name", firstName, "last name", lastName, "password", password1.hashCode())
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully updated!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "SUCCESS")

                        Toast.makeText(this, "Sign up is successful", Toast.LENGTH_SHORT)
                            .show()

                        // Redirect to Parents Home.
                        startActivity(Intent(this, ParentsHomeActivity::class.java))
                    }
                }

        } else {
            Toast.makeText(this, "Please fix the input errors", Toast.LENGTH_LONG).show()
        }
    }
}