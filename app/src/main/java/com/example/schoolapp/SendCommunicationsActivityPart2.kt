package com.example.schoolapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class SendCommunicationsActivityPart2 : AppCompatActivity(), RecipientSelectionClickListener {

    // This variable is used to access a Cloud Firestore instance.
    private val db = Firebase.firestore

    private val TAG = SendCommunicationsActivityPart2::class.qualifiedName

    // This list will contain the data for each class of the school.
    private var schoolClassList: MutableList<SchoolClass>? = null

    private var schoolClassAdapter : SchoolClassAdapter? = null

    // This list will contain the data for each student.
    private var studentList : MutableList<Student>? = null

    private var studentAdapter : StudentAdapter? = null

    private var recyclerView: RecyclerView? = null

    // This list will store the IDs of the selected recipient classes.
    private lateinit var listOfClassRecipients : ArrayList<String>

    // This list will store the recipient students as Student objects.
    private lateinit var listOfStudentRecipients : ArrayList<Student>

    // This list will store all class IDs of the school.
    private var classList : ArrayList<String>? = null

    // Views.
    private lateinit var subjectEditText : EditText
    private lateinit var contentEditText : EditText
    private lateinit var sendButton : Button
    private lateinit var recipientsTextView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the selection string from the Intent describing who the recipient is.
        val communicationsSelection : String = intent.getStringExtra(SendCommunicationsActivityPart1().EXTRA_SEND_COMMUNICATIONS_SELECTION)
        val contentEvent:String = intent.getStringExtra(SendCommunicationsActivityPart1().EXTRA_EVENT_MESSAGE)

        // First, get the IDs of all the classes present in the school.
        Log.d(TAG, "Getting class IDs...")
        classList = ArrayList()

        db.collection("students")
            .get()
            .addOnSuccessListener { documents ->

                // Iterate over each student document.
                for(document in documents){
                    var classId: String? = null

                    // Iterate over each data item about the student.
                    for(item in document.data) {
                        if(item.key == "class") {
                            classId = item.value.toString()

                            // If the class ID is not yet in classList, add it there.
                            if(!classList?.contains(classId)!!) {
                                classList?.add(classId)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
            .addOnCompleteListener {

                // Since the UI depends on the radio button selection, the layout is set here.
                if (communicationsSelection == "The parents of all students of all classes") {
                    setContentView(R.layout.activity_send_communications_part2_option1)

                    /* Check if the content is not empty, then the admin wants to send an event to individuals,
                    so we set the text content to be the message. */
                    if (contentEvent !=""){
                        val temp= findViewById<TextView>(R.id.contentEditText)
                        temp.text = contentEvent
                    }

                    subjectEditText = findViewById(R.id.subjectEditText)
                    contentEditText = findViewById(R.id.contentEditText)
                    sendButton = findViewById(R.id.sendButton)

                    // Get all parent usernames and store them in this list. These will be the recipients.
                    var recipients : ArrayList<String> = ArrayList()
                    db.collection("parents")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {

                                // The document ID is the parent username. So, add it to the recipients list.
                                recipients.add(document.id)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "Error getting documents: ", exception)
                        }
                        .addOnCompleteListener {

                            /* If the Send button is tapped, check that the fields are filled out,
                            and send the message to the recipients (store the new document in the
                            'communications' collection in the DB). */
                            sendButton.setOnClickListener {
                                var subject = subjectEditText.text.toString().trim()
                                var content = contentEditText.text.toString().trim()

                                // Only let the user proceed if all the fields were filled out.
                                var isValidInput = validateInput(subject, content, recipients)
                                if(isValidInput) {
                                    sendMessageToRecipients(subject, content, recipients)
                                }
                            }
                        }

                } else if(communicationsSelection == "The parents of all students of a specific class") {
                    setContentView(R.layout.activity_send_communications_part2_option2)

                    if (contentEvent !=null) {
                        val temp = findViewById<TextView>(R.id.contentEditText)
                        temp.text = contentEvent
                    }

                    subjectEditText = findViewById(R.id.subjectEditText)
                    recipientsTextView = findViewById(R.id.recipientsTextView)
                    contentEditText = findViewById(R.id.contentEditText)
                    sendButton = findViewById(R.id.sendButton)

                    /* Initialise the class list data for the RecyclerView.
                    Also, initialise the RecyclerView and the list for storing recipients.
                    Pass 2 as the option number parameter, so that the appropriate data will be added to the RecyclerView. */
                    initUIWithList(2)

                    // If the send button is tapped, execute the following.
                    sendButton.setOnClickListener {

                        var subject = subjectEditText.text.toString().trim()
                        var content = contentEditText.text.toString().trim()

                        // Get the recipients.
                        var recipientsString = recipientsTextView.text.toString()
                        var recipients: List<String> = recipientsString.split("\n")

                        /* The last element is empty as it is the '\n' character, which was added
                        to nicely display the recipients in the UI. This element is removed from the list. */
                        recipients = recipients.dropLast(1)

                        Log.d(TAG, "recipients: " + recipients)

                        /* Iterate over the recipient class IDs and schoolClassList.
                        Get the e-mails of the students in the recipient classes. */
                        var studentsOfRecipientClasses: ArrayList<String> = ArrayList()

                        for (recipientClassID in recipients) {
                            for (schoolClass in schoolClassList!!) {
                                if (recipientClassID == schoolClass.classID) {
                                    studentsOfRecipientClasses.addAll(schoolClass.students)
                                }
                            }
                        }
                        // Only let the user proceed if all the fields were filled out.
                        var isValidInput = validateInput(subject, content, studentsOfRecipientClasses)
                        if(isValidInput) {
                            Log.d(TAG, "studentsOfRecipientClasses: " + studentsOfRecipientClasses)

                            /* Iterate over the parents. Check if any of their kids' emails match any of the students' emails.
                            If so, add the parent to finalListOfRecipients. */
                            var finalListOfRecipients: ArrayList<String> = ArrayList()
                            db.collection("parents")
                                .get()
                                .addOnSuccessListener { documents ->

                                    // Iterate over each parent document.
                                    for (document in documents) {

                                        // Iterate over each data item about the parent.
                                        for (item in document.data) {
                                            if (item.key == "kids") {

                                                // Get the e-mails of their kids.
                                                val kidList = item.value as ArrayList<String>

                                                /* If any element of this list matches any element of
                                                studentsOfRecipientClasses, add the parent's username to finalListOfRecipients. */
                                                for (kidEmail in kidList) {
                                                    if (studentsOfRecipientClasses.contains(kidEmail)) {
                                                        finalListOfRecipients.add(document.id)
                                                        break
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d(TAG, "Error getting documents: ", exception)
                                }
                                .addOnCompleteListener {
                                    Log.d(TAG, "finalListOfRecipients: " + finalListOfRecipients)

                                    /* Send the message to the recipients
                                    (store the new document in the 'communications' collection in the DB). */
                                    sendMessageToRecipients(subject, content, finalListOfRecipients)
                                }
                        }
                    }
                } else {
                    setContentView(R.layout.activity_send_communications_part2_option3)
                    if (contentEvent !=null) {
                        val temp = findViewById<TextView>(R.id.contentEditText)
                        temp.text = contentEvent
                    }

                    subjectEditText = findViewById(R.id.subjectEditText)
                    recipientsTextView = findViewById(R.id.recipientsTextView)
                    contentEditText = findViewById(R.id.contentEditText)
                    sendButton = findViewById(R.id.sendButton)

                    /* Initialise the student list data for the RecyclerView.
                    Also initialise the RecyclerView, and the list for storing recipients.
                    Pass 3 as the option number parameter, so the appropriate data will be added to the RecyclerView. */
                    initUIWithList(3)

                    /* The admin can search recipients by student name or e-mail address to find the
                    students whose parents will be the recipients. */
                    sendButton.setOnClickListener {

                        var subject = subjectEditText.text.toString().trim()
                        var content = contentEditText.text.toString().trim()

                        // Get the recipients.
                        var recipientsString = recipientsTextView.text.toString()
                        var recipients: List<String> = recipientsString.split("\n")

                        /* The last element is empty as it is the '\n' character, which was added
                        to nicely display the recipients in the UI. This element is removed from the list. */
                        recipients = recipients.dropLast(1)

                        Log.d(TAG, "recipients: " + recipients)

                        // Store the recipient student e-mails.
                        var studentsOfRecipient: ArrayList<String> = ArrayList()

                        for (student in recipients) {
                            studentsOfRecipient.add(student)
                        }

                        // Only let the user proceed if all the fields were filled out.
                        var isValidInput = validateInput(subject, content, studentsOfRecipient)
                        if(isValidInput) {
                            Log.d(TAG, "studentsOfRecipientClasses: " + studentsOfRecipient)

                            /* Iterate over the parents. Check if any of their kids' emails match any of the students' emails.
                            If so, add the parent to finalListOfRecipients. */
                            var finalListOfRecipients: ArrayList<String> = ArrayList()
                            db.collection("parents")
                                .get()
                                .addOnSuccessListener { documents ->

                                    // Iterate over each parent document.
                                    for (document in documents) {

                                        // Iterate over each data item about the parent.
                                        for (item in document.data) {
                                            if (item.key == "kids") {

                                                // Get the e-mails of their kids.
                                                val kidList = item.value as ArrayList<String>

                                                /* If any element of this list matches any element of
                                                studentsOfRecipient, add the parent's username to finalListOfRecipients. */
                                                for (kidEmail in kidList) {
                                                    if (studentsOfRecipient.contains(kidEmail)) {
                                                        finalListOfRecipients.add(document.id)
                                                        break
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d(TAG, "Error getting documents: ", exception)
                                }
                                .addOnCompleteListener {
                                    Log.d(TAG, "finalListOfRecipients: " + finalListOfRecipients)

                                    /* Send the message to the recipients
                                    (store the new document in the 'communications' collection in the DB). */
                                    sendMessageToRecipients(subject, content, finalListOfRecipients)
                                }
                        }
                    }
                }
            }
    }

    /**
     * Initialises the list data, the RecyclerView, the list of recipients,
     * and adds a change listener to the search edit text.
     */
    private fun initUIWithList(optionNum : Int) {

        if(optionNum == 2) {
            Log.d(TAG, "Starting initSchoolClassList...")
            initSchoolClassList()
            listOfClassRecipients = ArrayList<String>()
        } else {
            Log.d(TAG, "Starting initStudentList...")
            initStudentList()
            listOfStudentRecipients = ArrayList<Student>()
        }

        // Initialise the RecyclerView with the correct data depending on the messaging option.
        initRecyclerView(optionNum)

        // Add a change listener to the search edit text.
        var editText : EditText = findViewById(R.id.search_edit_text)

        if(optionNum == 2) {

            editText.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                }

                /* After the text is changed, filter the list.
                Editable s is the content of the search edit text. */
                override fun afterTextChanged(s: Editable) {
                    filterClasses(s.toString())
                }
            });
        } else {
            editText.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                }

                /* After the text is changed, filter the list.
                Editable s is the content of the search edit text. */
                override fun afterTextChanged(s: Editable) {
                    filterStudents(s.toString())
                }
            });
        }
    }

    /**
     * Fills schoolClassList with school class data, which will be added to the RecyclerView.
     */
    private fun initSchoolClassList() {

        Log.d(TAG, "initSchoolClassList started")
        schoolClassList = ArrayList()

        // For each class, find the students who go to that class.
        for(classId in classList!!) {

            var classStudents : ArrayList<String> = ArrayList()

            db.collection("students")
                .get()
                .addOnSuccessListener { documents ->

                    // Iterate over each student document.
                    for(document in documents){
                        var email: String? = null

                        var goesToThisClass = false

                        // Iterate over each data item about the student. Get their email.
                        for(item in document.data) {

                            if(item.key == "email") {
                                email = item.value.toString()
                            }

                            Log.d(TAG, "classId: $classId")
                            Log.d(TAG, "item.value.toString(): " + item.value.toString())

                            // Check if the student goes to this class.
                            if(item.key == "class" && item.value.toString() == classId) {
                                Log.d(TAG, "MATCH. classId: $classId  . item.value.toString(): " + item.value.toString())
                                goesToThisClass = true
                            }
                        }
                        // If the student goes to this class, add their email to the list of this class.
                        if(goesToThisClass) {
                            Log.d(TAG, "goesToThisClass: " + goesToThisClass)
                            if (email != null) {
                                classStudents.add(email)
                            }
                        }
                    }
                    Log.d(TAG, "classStudents: " + classStudents)
                    if (classStudents != null) {
                        Log.d(TAG, "classStudents[0]: " + classStudents[0])
                    }

                    // Add the list of students belonging to the class to schoolClassList.
                    classStudents?.let {
                        SchoolClass(classId,
                            it
                        )
                    }?.let { (schoolClassList as ArrayList<SchoolClass>).add(it) }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }
        }
    }

    /**
     * Fills studentList with student data, which will be added to the RecyclerView.
     */
    private fun initStudentList() {

        Log.d(TAG, "initStudentList started")

        studentList = ArrayList<Student>()

        db.collection("students")
            .get()
            .addOnSuccessListener { documents ->

                // Iterate over each student document.
                for(document in documents){
                    var email: String? = null
                    var classId: String? = null
                    var firstName: String? = null
                    var lastName: String? = null

                    // Iterate over each data item about the student.
                    for(item in document.data) {
                        if(item.key == "first name") {
                            firstName = item.value.toString()
                        }
                        if(item.key == "last Name") {
                            lastName = item.value.toString()
                        }
                        if(item.key == "class") {
                            classId = item.value.toString()
                            Log.d(TAG, "Class Num $classId")

                            // If the class ID is not yet in classList, add it there.
                            if(!classList?.contains(classId)!!) {
                                classList?.add(classId)
                            }
                        }
                        if(item.key == "email") {
                            email = item.value.toString()
                            Log.d(TAG, "Email $email")
                        }
                    }
                    var studentName = firstName + " " + lastName
                    (studentList as ArrayList<Student>).add(Student(studentName, email.toString()))
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }

    }

    /**
     * Initialises the recycler view using the specified list of data and the adapter.
     */
    private fun initRecyclerView(optionNum : Int) {

        recyclerView = findViewById(R.id.search_recycler_view)
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager = LinearLayoutManager(this)

        if(optionNum == 2) {
            schoolClassAdapter = schoolClassList?.let { SchoolClassAdapter(it, this, this) }
            recyclerView!!.adapter = schoolClassAdapter
        } else {
            studentAdapter = studentList?.let { StudentAdapter(it, this, this) }
            recyclerView!!.adapter = studentAdapter
        }
    }

    /**
     * This function is called every time a letter is typed or deleted from the search box for searching class IDs.
     * A class can be searched based on class ID e.g. 7.T
     */
    private fun filterClasses(searchQuery : String) {
        var filteredList = ArrayList<SchoolClass>()

        // Iterate over each SchoolClass object.
        for (item : SchoolClass in this.schoolClassList!!) {

            // If a class ID contains the search query, store the SchoolClass object in the filtered list.
            if(item.classID.toLowerCase().contains(searchQuery.toLowerCase())) {
                filteredList.add(item)
            }
        }
        schoolClassAdapter?.filterList(filteredList)
    }

    /**
     * This function is called every time a letter is typed or deleted from the search box for searching students.
     * A student can be searched based on name or e-mail address.
     */
    private fun filterStudents(searchQuery : String) {
        var filteredList = ArrayList<Student>()

        // Iterate over each Student object.
        for (item : Student in this.studentList!!) {

            // If a student name or email contains the search query, store the Student object in the filtered list.
            if(item.name.toLowerCase().contains(searchQuery.toLowerCase()) || item.email.toLowerCase().contains(searchQuery.toLowerCase())) {
                filteredList.add(item)
            }
        }
        studentAdapter?.filterList(filteredList)
    }

    /**
     * This function implements the addClassRecipient() function
     * of the RecipientSelectionClickListener interface.
     * If a recipient is tapped, it is added to the list of recipients.
     */
    override fun addClassRecipient (classID : String) {

        /* If the recipient is not yet added to the list of recipients, then add it.
        Otherwise, tell the user they already added it. */
        if(!listOfClassRecipients.contains(classID)) {

            // Add the recipient to the array list and the text view.
            listOfClassRecipients.add(classID)

            var recipients: TextView = findViewById(R.id.recipientsTextView)
            if(recipients.text.toString() == "") {
                recipients.text = classID + "\n"
            } else {
                recipients.text = recipients.text.toString() + classID + "\n"
            }
        } else {
            Toast.makeText(this, "The tapped item is already in the list of recipients" , Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This function implements the addStudentRecipient() function
     * of the RecipientSelectionClickListener interface.
     * If a recipient is tapped, it is added to the list of recipients.
     */
    override fun addStudentRecipient(s: Student) {

        /* If the recipient is not yet added to the list of recipients, then add it.
        Otherwise, tell the user they already added it. */
        if(!listOfStudentRecipients.contains(s)) {

            // Add the recipient to the array list and the text view.
            listOfStudentRecipients.add(s)

            var recipients: TextView = findViewById(R.id.recipientsTextView)
            if(recipients.text.toString() == "") {
                recipients.text = s.email + "\n"
            } else {
                recipients.text = recipients.text.toString() + s.email + "\n"
            }
        } else {
            Toast.makeText(this, "The tapped item is already in the list of recipients" , Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * If the user taps the backspace button, the last class recipient is removed from the list of recipients.
     */
    fun removeClassRecipient(view: View) {

        /* Only remove something if there are items to be removed.
        If the list is empty, tell the user. */
        if(listOfClassRecipients.size > 0) {

            // Remove the recipient from the array list and the text view.
            listOfClassRecipients.removeAt(listOfClassRecipients.size - 1)

            var recipients: TextView = findViewById(R.id.recipientsTextView)
            recipients.text = ""

            for(recipient : String in listOfClassRecipients) {
                if(recipients.text.toString() == "") {
                    recipients.text = recipient + "\n"
                } else {
                    recipients.text = recipients.text.toString() + recipient + "\n"
                }
            }
        } else {
            Toast.makeText(this, "There are no recipients" , Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * If the user taps the backspace button, the last student recipient is removed from the list of recipients.
     */
    fun removeStudentRecipient(view: View) {

        /* Only remove something if there are items to be removed.
        If the list is empty, tell the user. */
        if(listOfStudentRecipients.size > 0) {

            // Remove the recipient from the array list and the text view.
            listOfStudentRecipients.removeAt(listOfStudentRecipients.size - 1)

            var recipients: TextView = findViewById(R.id.recipientsTextView)
            recipients.text = ""

            for(s : Student in listOfStudentRecipients) {
                if(recipients.text.toString() == "") {
                    recipients.text = s.email + "\n"
                } else {
                    recipients.text = recipients.text.toString() + s.email + "\n"
                }
            }
        } else {
            Toast.makeText(this, "There are no recipients" , Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Checks if the subject, content, and recipients are entered before a message is sent.
     */
    private fun validateInput(subject : String, content : String, recipients : ArrayList<String>) : Boolean {

        // The error message to display to the user (if there are errors) will be stored in this string.
        var error : String? = ""

        // Check if the subject and content were entered.
        if(subject.isEmpty()) {
            error += "You have not entered a subject. "
        }
        if(content.isEmpty()) {
            error += "You have not entered any message content. "
        }

        // If there are no recipients selected, tell the user.
        if (recipients == null || recipients.isEmpty()) {
            error += "You have not chosen any recipients. "
        }

        // If a certain type of input is missing, tell the user.
        if(subject.isEmpty() || content.isEmpty() || recipients == null || recipients.isEmpty()) {
            Toast.makeText(this, error , Toast.LENGTH_SHORT).show()
            return false
        }
        // Otherwise, send the user's entered message.
        else {
            return true
        }
    }

    /**
     * Sends the given message to the specified list of recipients. Adds the message to the DB.
     */
    private fun sendMessageToRecipients(subject : String, content : String, listOfRecipients : ArrayList<String>) {

        Toast.makeText(this, "Sending to the recipients..." , Toast.LENGTH_SHORT).show()

        // Get the current date and time.
        var currentDateAndTime : Timestamp = Timestamp.now()

        // The communication document to be added to the DB.
        val communication = hashMapOf(
            "Date" to currentDateAndTime,
            "Message" to content,
            "Subject" to subject,
            "To" to listOfRecipients
        )

        /* Send the message to the recipients
        (store the new document in the 'communications' collection in the DB). */
        db.collection("communications")
            .add(communication)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
                Toast.makeText(this, "The message is successfully sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
            .addOnCompleteListener {

                // Redirect to Admin Home.
                intent = Intent(this, AdminHomeActivity::class.java).apply {

                    // If the currentUser value is 0, then the user is the admin.
                    putExtra("currentUser","0")
                }
                startActivity(intent)
            }
    }
}