package com.example.schoolapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EditStudent : AppCompatActivity() {

    // Access a Cloud Firestore instance
    private val db = Firebase.firestore

    // Views.
    private lateinit var textInputFirstName : EditText
    private lateinit var textInputLastName : EditText
    private lateinit var textInputClassId : EditText
    private lateinit var textInputParent1Num : EditText
    private lateinit var textInputParent2Num : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_student_profile)

        val documentID = intent.getStringExtra("documentId")
        textInputFirstName = findViewById(R.id.student_first_name)
        textInputLastName = findViewById(R.id.student_last_name)
        textInputClassId = findViewById(R.id.student_class_text_view)
        var email : TextView = findViewById(R.id.student_email_text_view)
        textInputParent1Num = findViewById(R.id.student_parent1_phone_text_view)
        textInputParent2Num = findViewById(R.id.student_parent2_phone_text_view)

        db.collection("students")
            .document(documentID)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("TAG", "Show the document ${document::class.simpleName}")
                    // Iterate over each data item about the student
                    textInputFirstName.setText(document.get("first name").toString())
                    textInputLastName.setText(document.get("last Name").toString())
                    textInputClassId.setText(document.get("class").toString())
                    textInputParent1Num.setText(document.get("parent1 phone").toString())
                    textInputParent2Num.setText(document.get("parent2 phone").toString())
                    email.text = document.get("email").toString()

                    val submit = findViewById<Button>(R.id.submit_student_change)
                    submit.setOnClickListener {
                        var firstName = textInputFirstName.text.toString()
                        var lastName = textInputLastName.text.toString()
                        var classid = textInputClassId.text.toString()
                        var phoneNum1 = textInputParent1Num.text.toString()
                        var phoneNum2 = textInputParent2Num.text.toString()
                        var stuEmail = email.text.toString()

                        Log.d("TAG", "First Name: $firstName")
                        val student = hashMapOf(
                            "first name" to firstName,
                            "last Name" to lastName,
                            "class" to classid,
                            "email" to stuEmail,
                            "parent1 phone" to phoneNum1,
                            "parent2 phone" to phoneNum2
                            )
                        db.collection("students")
                            .document(documentID)
                            .set(student)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "The student has been saved successfully.", Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(
                                    this,
                                    ManageStudentActivity::class.java
                                )

                                // Redirect to ManageStudentActivity as the admin (currentUser is 0).
                                intent.putExtra("currentUser", "0")
                                startActivity(intent)
                            }
                    }
                }
                else {
                    Log.d("TAG", "No this student $documentID")
                }
            }
    }
}