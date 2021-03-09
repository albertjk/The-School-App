package com.example.schoolapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.manage_student.*

class ManageStudentActivity : AppCompatActivity(){

    // This variable is used to access a Cloud Firestore instance.
    private val db = Firebase.firestore

    private val TAG = ManageStudentActivity::class.qualifiedName

    private var recyclerView: RecyclerView? = null

    private var studentNameList : MutableList<StudentList> = ArrayList()

    private lateinit var studentAdapter : StudentListAdapter
    @SuppressLint("WrongViewCast")
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_student)

        recyclerView = findViewById(R.id.student_List)

        val addStudentBtn = findViewById<FloatingActionButton>(R.id.add_student_btn)
        addStudentBtn.setOnClickListener {
            val intent = Intent(
                this, AddStudentActivity::class.java
            )
            startActivity(intent)
        }
        initData()
        setupAdapter()
        student_search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                studentAdapter.filter.filter(newText)
                return false
            }

        })
    }

    /**
     * Gets student data from the database and stores them in studentNameList.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun initData(){
        studentNameList = ArrayList()

        db.collection("students")
            .get()
            .addOnSuccessListener { documents ->
                for(document in documents){

                    var email: String? = null
                    var classId: String? = null
                    var firstName: String? = null
                    var lastName: String? = null

                    // Iterate over each data item about the student
                    for(item in document.data) {
                        if(item.key == "first name") {
                            firstName = item.value.toString()
                        }
                        if(item.key == "last Name") {
                            lastName = item.value.toString()
                        }
                        if(item.key == "class") {
                            classId = item.value.toString()
                            Log.d("TAG", "Class Num $classId")
                        }
                        if(item.key == "email") {
                            email = item.value.toString()
                            Log.d("TAG", "Email $email")
                        }
                    }
                    var studentName = "$firstName $lastName"
                    (studentNameList as ArrayList<StudentList>).add(StudentList(
                        studentName,
                        email.toString(),
                        classId.toString()))
                    Log.d("TAG", "StudentList: $studentNameList")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
            .addOnCompleteListener {

                // Sort the studentNameList in the array list into descending order by name.
                studentNameList!!.sortWith(compareBy{ it.studentName })
                studentNameList!!.reverse()

                // After getting the data, initialise the recycler view.
                setupAdapter()
            }
    }

    /**
     * Sets the layout manager and the adapter of the recycler view.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAdapter() {

        // The recyclerView needs an adapter.
        studentAdapter = studentNameList.let { StudentListAdapter(it, this) }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = studentAdapter
        Log.d("TAG", "StudentList: $studentNameList")
    }
}