package com.example.schoolapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

/**
 * EventsActivity is responsible for the representation of the events to parents and students
 * and to give the ability to admin to manage them.
 */
class EventsActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRestart() {
        currentUser=intent.getStringExtra("currentUser")
        super.onRestart()
        initData()
        // When the BACK BUTTON is pressed, the activity on the stack is restarted
    }

    // Access a Cloud Firestore instance
    private val db = Firebase.firestore

    // Used for messages in logs
    private val TAG = StudentProfileActivity::class.qualifiedName
    private var recyclerView: RecyclerView? = null

    // This list will contain the data for each row.
    private var  eventList: ArrayList<Events>? = null

    /* This variable will store the type of user: student/parent or admin.
     "1" means student/parent. "0" means admin. */
    private var currentUser : String? = null

    @SuppressLint("WrongViewCast")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)
        recyclerView = findViewById(R.id.recyclerView)

        // Find the id for the button that adds new event
        var addEvent:Button = findViewById(R.id.btn_addEvent)

        // Receive the currentUser flag from the previous activity
        currentUser = intent.getStringExtra("currentUser")

        // If the current user is an admin, let them add a new event.
        if (currentUser == "0") {
            addEvent.visibility = View.VISIBLE
        }
        else {
            addEvent.visibility = View.INVISIBLE
        }
        // Start the AddNewEvent activity if the correspond button is clicked
        addEvent.setOnClickListener{
            val intent = Intent(this, AddNewEventActivity::class.java)

            // Send '-1' if the event will be new
            intent.putExtra("documentId","-1")
            intent.putExtra("currentUser",currentUser)
            currentUser="0"
            this.startActivity(intent)
        }
        /* Initialise the data before initialising the RecyclerView.
        eventList may be empty before initialising the RecyclerView.
        It depends on the data from DB*/
        initData()
    }

    /**
     * Receive all the events from the database anc create an arraylist with their details.
     */
    fun initData() {

        // Initialise the event list
        eventList = ArrayList()
        var content:String?=null
        var date:Date?=null
        var start:String?=null
        var end:String?=null
        var shortDescription:String?=null
        var title:String?=null
        var location:String?=null
        var coordinates:GeoPoint?=null
        var coordinatesLongitude: Double?=null
        var coordinatesLatitude:Double?=null

        // Receive all the events from the DB and add them to the list of events
        db.collection("events")
            .get()
            .addOnSuccessListener { documents ->

                // For each event in the DB
                for (document in documents) {

                    // Iterate over each data item about the event
                    for(item in document.data) {
                        if(item.key == "Content") {
                            content = item.value.toString()
                        }
                        if(item.key == "Date") {
                            if (item.value != null){
                            var timestamp = item.value as Timestamp?
                            if (timestamp != null) {
                                date = timestamp.toDate()
                            }
                            }
                        }
                        if(item.key == "Start") {
                            start= item.value.toString()
                        }
                        if(item.key == "End") {
                            end = item.value.toString()
                        }
                        if(item.key == "Location") {
                            location = item.value.toString()
                        }
                        if(item.key == "ShortDescription") {
                            shortDescription = item.value.toString()
                        }
                        if(item.key == "Title") {
                            title = item.value.toString()
                        }
                        if(item.key == "Coordinates") {
                            coordinatesLatitude = (item.value as GeoPoint).latitude
                            coordinatesLongitude = (item.value as GeoPoint).longitude
                            coordinates = GeoPoint(coordinatesLatitude!!, coordinatesLongitude!!)
                        }
                    }
                    // Add the event in the list
                    date?.let {
                        Events(
                            title.toString(),
                            it,
                            start.toString(),
                            end.toString(),
                            content.toString(),
                            location.toString(),
                            coordinates,
                            shortDescription.toString())
                    }?.let { (eventList as ArrayList<Events>).add(it) }

                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
            .addOnCompleteListener{

                // Sort the events in the array list into descending order by date.
                eventList!!.sortWith(compareBy{ it.date })
                eventList!!.reverse()

                // After getting the data, initialise the recycler view.
                initRecyclerView()
            }
    }

    private fun initRecyclerView() {
        val eventsAdapter = eventList?.let { currentUser?.let { it1 -> EventsAdapter(it,this, it1) } }
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = eventsAdapter
    }
}

