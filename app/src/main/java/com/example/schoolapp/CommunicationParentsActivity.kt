package com.example.schoolapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import java.util.*

/**
 * CommunicationParentsActivity is responsible for the representation of the communications from
 * admin to parents.
 */
class CommunicationParentsActivity : AppCompatActivity() {
    private var recyclerView: RecyclerView? = null

    // Access a Cloud Firestore instance
    private val db = Firebase.firestore

    // Variable used to print messages in Log
    private val TAG = CommunicationParentsActivity::class.qualifiedName
    var EXTRA_PARENT_HOME: String = "com.example.schoolapp.EXTRA_PARENT_HOME"

    // This list will contain the data for each row.
    private var communicationParentsList: MutableList<CommunicationParents>? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communicationparents)

        // Find the id of the recycler view in the layout
        recyclerView = findViewById(R.id.recyclerView)

        /* Initialise the data before initialising the RecyclerView.
        communicationParents List may be empty before initialising the RecyclerView.
        It depends on the date from DB*/
        initData()
        initRecyclerView()
    }

    /**
     * Gets data from the DB and initialises communicationParentsList.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initData() {

        // Initialise the list of the communication messages
        communicationParentsList = ArrayList()
        var content:String?=null
        var date: Date?=null
        var subject:String?=null
        var recipients = ArrayList<String>()

        // Receive from the DB all the communications
        db.collection("communications")
            .get()
            .addOnSuccessListener { documents ->
                // For each communication
                for (document in documents) {
                    // Iterate over each data item about the communication and receive its details
                    for (item in document.data) {
                        if (item.key == "Message") {
                            content = item.value.toString()
                        }
                        if(item.key == "Date") {
                           var timestamp = item.value as Timestamp?
                           if (timestamp != null) {
                              date = timestamp.toDate()
                            }
                        }
                        if (item.key == "Subject") {
                            subject = item.value.toString()
                        }

                    }
                    // Get the username of the current user (who is a parent)
                    val parentUsername : String = intent.getStringExtra(com.example.schoolapp.ParentsHomeActivity().EXTRA_PARENT_HOME)

                    /* If the current communication contains the current user to its recipients
                    continue to find all the recipients */
                    if ((document.data["To"] as ArrayList<String>).contains(parentUsername)) {
                        // For each recipient in this communication
                        for (rec in document.data["To"] as ArrayList<String>) {

                            // Name variable is for each recipient
                            var name = ""
                            var found = false
                            db.collection("students")
                                .whereEqualTo("email", rec)
                                .get()

                                // Find the first and last name
                                .addOnSuccessListener { documents ->
                                    found = true
                                    for (document in documents) {
                                        name = ""
                                        for (item in document.data) {
                                        }
                                        name =
                                            "" + document.data["first name"] + " " + document.data["last Name"]
                                        // Add the name to the recipients list
                                        recipients.add(name)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d(TAG, "get failed with ", exception)
                                }
                                .addOnCompleteListener {
                                    // Find the parent's document
                                    db.collection("parents")
                                        .whereEqualTo("username", rec)
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            found = true
                                            for (document in documents) {
                                                name = ""
                                                for (item in document.data) {
                                                }
                                                name =
                                                    "" + document.data["first name"] + " " + document.data["last name"]
                                                // Add the name to the recipients list
                                                recipients.add(name)
                                            }
                                        }
                                        .addOnCompleteListener {
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.d(TAG, "get failed with ", exception)
                                        }
                                }
                        }
                        // Add the communication message to the list with the communications
                        date?.let {
                            CommunicationParents(
                                subject.toString(),
                                content.toString(),
                                it,
                                recipients.joinToString()
                            )
                        }?.let { (communicationParentsList as ArrayList<CommunicationParents>).add(it) }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
            .addOnCompleteListener{
                // Sort the communications in the array list into descending order by date.
                communicationParentsList!!.sortWith(compareBy{ it.date })
                communicationParentsList!!.reverse()
                initRecyclerView()
            }
    }

    /**
     * Sets the layout manager and the adapter of the recycler view.
     */
    private fun initRecyclerView() {

        // To use the recyclerView we need a recycler adapter.
        val communicationParentsAdapter = communicationParentsList?.let { CommunicationParentsAdapter(it,this) }
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = communicationParentsAdapter
    }
}