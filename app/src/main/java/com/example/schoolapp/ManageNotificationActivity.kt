package com.example.schoolapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class ManageNotificationActivity : AppCompatActivity(){

    // This variable is used to access a Cloud Firestore instance
    private val db = Firebase.firestore

    private val TAG = ManageNotificationActivity::class.qualifiedName

    private var recyclerView: RecyclerView? = null

    // This list will contain the data for each row.
    private var notificationList: ArrayList<Notification>? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerView)
        val addNotificationBtn = findViewById<FloatingActionButton>(R.id.add_notification_btn)
        addNotificationBtn.setOnClickListener {
            val intent = Intent(
                this, AddNotificationActivity::class.java
            )
            Log.d("TAG", "The bottom sucessed touched")
            startActivity(intent)
        }
        /* Initialise the data before initialising the RecyclerView.
        notificationList must not be empty before initialising the RecyclerView. */
        initData()

    }
    /**
     * Gets the notification data from the DB and stores them in the notificationList array list.
     */
    private fun initData() {

        // Notification data retrieved from the DB will be stored in this array list.
        notificationList = ArrayList()

        val sdf = SimpleDateFormat("MM/dd/yyyy")

        db.collection("notifications")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")

                    // For each notification, get its details: the title, content, and date.
                    var title : String? = null
                    var content : String? = null
                    var date : Date? = null

                    for (item in document.data) {
                        if(item.key == "title") {
                            title = item.value as String?
                        } else if(item.key == "content") {
                            content = item.value as String?
                        } else if(item.key == "date") {
                            var timestamp = item.value as Timestamp?
                            if (timestamp != null) {
                                date = timestamp.toDate()
                            }
                        }
                    }
                    (notificationList as ArrayList<Notification>).add(Notification(title, content, date))
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
            .addOnCompleteListener {

                // Sort the notifications in the array list into descending order by date.
                notificationList!!.sortWith(compareBy{ it.date })
                notificationList!!.reverse()

                // After getting the data, initialise the recycler view.
                initRecyclerView()
            }
    }

    /**
     * Sets the layout manager and the adapter of the recycler view.
     */
    private fun initRecyclerView() {

        // The recyclerView needs an adapter.
        val manageNotificationAdapter = notificationList?.let { ManageNotificationAdapter(it, this) }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = manageNotificationAdapter
    }
}