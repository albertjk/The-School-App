package com.example.schoolapp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StudentProfileActivity : AppCompatActivity() {

    // Access a Cloud Firestore instance
    private val db = Firebase.firestore

    private val TAG = StudentProfileActivity::class.qualifiedName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        var firstName : TextView = findViewById(R.id.student_first_name_text_view)
        var lastName : TextView = findViewById(R.id.student_last_name_text_view)
        var classId : TextView = findViewById(R.id.student_class_text_view)
        var email : TextView = findViewById(R.id.student_email_text_view)
        var parent1PhoneNum : TextView = findViewById(R.id.student_parent1_phone_text_view)
        var parent2PhoneNum : TextView = findViewById(R.id.student_parent2_phone_text_view)

        // From the intent, get the e-mail address of the student who is logged in.
        val studentEmail : String = intent.getStringExtra(com.example.schoolapp.StudentHomeActivity().EXTRA_STUDENT_PROFILE)

        // Find the student based on their e-mail address, which is their unique identifier.
        db.collection("students")
            .whereEqualTo("email", studentEmail)
            .get()
            .addOnSuccessListener { documents ->

                for (document in documents) {

                    // Iterate over each data item about the student
                    for(item in document.data) {
                        if(item.key == "first name") {
                            firstName.text = item.value.toString()
                        }
                        if(item.key == "last Name") {
                            lastName.text = item.value.toString()
                        }
                        if(item.key == "class") {
                            classId.text = item.value.toString()
                        }
                        if(item.key == "email") {
                            email.text = item.value.toString()
                        }
                        if(item.key == "parent1 phone") {
                            parent1PhoneNum.text = item.value.toString()
                        }
                        if(item.key == "parent2 phone") {
                            parent2PhoneNum.text = item.value.toString()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }
}