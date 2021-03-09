package com.example.schoolapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_add_new_event.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * AddNewEvent Activity is activated when admin chooses to create a new event.
 * The appropriate layout appears and admin can fill the fields and save the event.
 */
class AddNewEventActivity : AppCompatActivity() {

    // Defines the desired format of date and time.
    private var format = SimpleDateFormat("dd-MM-yyyy")
    private var timeFormat = SimpleDateFormat("hh:mm a")

    // This variable is used to access a Cloud Firestore instance
    private val db = Firebase.firestore

    // Variable for testing purposes (appears in logs)
    private val TAG = AddNewEventActivity::class.qualifiedName

    /* This variable will store the type of user: student/parent or admin.
	"1" means student/parent. "0" means admin. Of course, only the admin can add new events.
	This value is only needed to redirect to the correct EventsActivity screen
	(what the admin should see) after adding a new event. */
    private var currentUser : String? = null

    // Variables to assign the selected values in the form
    private var selectedYear : Int? = null
    private var selectedMonth : Int? = null
    private var selectedDay : Int? = null
    private var selectedHour : Int? = null
    private var selectedMinute : Int? = null

    /* The selected date will be stored in this variable so that it can be converted to
    a Timestamp object to be stored in the DB. */
    private var selectedTimestamp : Date? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_event)

        // Find the ids of the textfields in the layout for add new event
        var titleTextView: TextView = findViewById(R.id.txt_title)
        var dateTextView: TextView = findViewById(R.id.btn_date)
        var startdateTextView: TextView = findViewById(R.id.btn_start)
        var enddateTextView: TextView = findViewById(R.id.btn_end)
        var descriptionTextView: TextView = findViewById(R.id.txt_description)
        var locationTextView: TextView = findViewById(R.id.txt_location)
        var shortDescriptionTextView: TextView = findViewById(R.id.txt_shortDescription)
        var latitude: TextView = findViewById(R.id.geolocation_latitude)
        var longitute: TextView = findViewById(R.id.geolocation_longitude)
        var label:TextView = findViewById(R.id.textView2)

        // Get the document id from previous activity
        var documentId:String=intent.getStringExtra("documentId")

        // Get the currentUser flag from previous activity
        currentUser = intent.getStringExtra("currentUser")

        // If documentId is not -1, it means the event is not new, so it will be edited.
        if (documentId!="-1" ){

            // Change the label of the layout
            label.text="EDIT EVENT"

            // Find for each event its details and print them to the correspond fields
            db.collection("events").document(documentId)
                .get()
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
                .addOnSuccessListener {document->
                    // Set in title field the value received from DB
                    titleTextView.text =document.data?.get("Title").toString()

                    // Get the timestamp from the DB and only keep the date.
                    var timestamp = document.data?.get("Date") as Timestamp
                    val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
                    val sdf = SimpleDateFormat("dd-MM-yyyy")
                    val netDate = Date(milliseconds)
                    val date = sdf.format(netDate).toString()

                    // Set the values received from DB to the correspond fields in the view
                    dateTextView.text = date
                    enddateTextView.text = document.data?.get("End").toString()
                    startdateTextView.text = document.data?.get("Start").toString()
                    shortDescriptionTextView.text = document.data?.get("ShortDescription").toString()
                    descriptionTextView.text = document.data?.get("Content").toString()
                    locationTextView.text = document.data?.get("Location").toString()
                    latitude.text = (document.data?.get("Coordinates") as GeoPoint).latitude.toString()
                    longitute.text=(document.data?.get("Coordinates") as GeoPoint).longitude.toString()
                }
        }

        // When the admin clicks the 'save' button, check if the mandatory fields are filled or not e.g. Title, Date etc.
        val submit = findViewById<Button>(R.id.submitEvent)
        submit.setOnClickListener {
            // If there was an input error, let the user know.
            if (TextUtils.isEmpty(titleTextView.text) || TextUtils.isEmpty(locationTextView.text) ||
                TextUtils.isEmpty(dateTextView.text) || TextUtils.isEmpty(startdateTextView.text) ||
                TextUtils.isEmpty(enddateTextView.text) || TextUtils.isEmpty(descriptionTextView.text) ||
                TextUtils.isEmpty(shortDescriptionTextView.text) || TextUtils.isEmpty(latitude.text) ||
                TextUtils.isEmpty(longitute.text)) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Alert")
                builder.setMessage("You need to complete all the fields.")
                builder.setNeutralButton("Okay") { _, _ ->
                }
                builder.show()
            }
            // If there was no input error, proceed.
            else {
                // Save the values from the fields in variables
                val title = titleTextView.text.toString()
                val location = locationTextView.text.toString()
                val l = LocalDate.parse("14-02-2018", DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                val start = startdateTextView.text.toString()
                val end = enddateTextView.text.toString()
                val description=descriptionTextView.text.toString()
                val shortDescription=shortDescriptionTextView.text.toString()
                var latitude=latitude.text.toString().toDouble()
                val longitude=longitute.text.toString().toDouble()

                /* If the latitude is outside of the range of [-90, 90] and the longitude is outside
                of the range of [-180, 180], tell the user. */
                var message : String? = ""
                if(latitude < -90 || latitude > 90) {
                    message += "The latitude must be between -90 and 90. "
                }
                if(longitude < -180 || longitude > 180) {
                    message += "The longitude must be between -180 and 180."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                /* If the user did not update the hour and minute, the time will be midday in the timestamp automatically. */
                if(selectedHour == null && selectedMinute == null) {
                    selectedHour = 0
                    selectedMinute = 0
                }
                /* Convert the date to a Date object.
                Must subtract 1900 to get the correct year, as specified in the documentation. */
                selectedTimestamp = selectedYear?.minus(1900)?.let { it1 -> selectedMonth?.let { it2 ->
                    selectedDay?.let { it3 ->
                        selectedHour?.let { it4 ->
                            selectedMinute?.let { it5 ->
                                Date(it1,
                                    it2, it3, it4, it5
                                )
                            }
                        }
                    }
                }
                }
                // If the latitude and longitude values were correct, proceed.
                if ((latitude > -90 && latitude < 90) && (longitude > -180 && longitude < 180)) {
                    var coordinates:GeoPoint= GeoPoint(latitude,longitude)
                    val event = hashMapOf(
                        "Title" to title,
                        "Location" to location,
                        "Date" to selectedTimestamp?.let { it1 -> Timestamp(it1) },
                        "Start" to start,
                        "End" to end,
                        "Content" to description,
                        "ShortDescription" to shortDescription,
                        "Coordinates" to coordinates
                    )
                    // If documentId is -1, it means the event is new, so it will be added to the DB with an autogenerated id.
                    if (intent.getStringExtra("documentId")=="-1"){
                        db.collection("events")
                            .add(event)
                            .addOnSuccessListener {
                                EventsActivity().initData()
                                Toast.makeText(
                                    this,
                                    "The event has been saved successfully.", Toast.LENGTH_SHORT
                                ).show()
                                // Redirect to the Events screen.
                                val intent = Intent(
                                    this,
                                    EventsActivity::class.java
                                )
                                // Redirect to EventsActivity as the admin (currentUser is 0).
                                intent.putExtra("currentUser", "0")
                                startActivity(intent)
                            }
                    }
                    // Otherwise, the event is being edited, so it will be updated in the DB.
                    else{
                        db.collection("events").document(intent.getStringExtra("documentId"))
                            .set(event)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "The event has been saved successfully.", Toast.LENGTH_SHORT
                                ).show()
                                // Redirect to the Events screen.
                                val intent = Intent(
                                    this,
                                    EventsActivity::class.java
                                )
                                // Redirect to EventsActivity as the admin (currentUser is 0).
                                intent.putExtra("currentUser", "0")
                                startActivity(intent)
                            }
                    }
                }
            }
        }

        // The date field is clicked, so a date picker dialog appears
        btn_date.setOnClickListener {
            val now = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                R.style.PickDialogTheme,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    // Receive the selected year/month/day and set the date as text to the field
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(Calendar.YEAR, year)
                    selectedDate.set(Calendar.MONTH, month)
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = dayOfMonth
                    val date = format.format(selectedDate.time)
                    btn_date.setText(date)
                },
                // Set the date of the dialog (when opens) to be the current date
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            // Disallow the ability to select a past date for a new event
            datePicker.datePicker.minDate = Calendar.getInstance().timeInMillis
            // Show the date dialog picker
            datePicker.show()
        }
        // The time start field is clicked, so a time picker dialog appears
        btn_start.setOnClickListener {
            val now = Calendar.getInstance()
            val timePicker = TimePickerDialog(
                this,
                R.style.PickDialogTheme,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    // Receive the selected hour/minutes and set the time as text to the field
                    val selectedTime = Calendar.getInstance()
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedTime.set(Calendar.MINUTE, minute)
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    btn_start.setText(timeFormat.format(selectedTime.time))
                },
                // Set the hour & minutes appear in the dialog to be the current time
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
            )
            // Show the time picker dialog
            timePicker.show()
        }

        // The time end field is clicked, so a time picker dialog appears
        btn_end.setOnClickListener {
            val now = Calendar.getInstance()
            val timePicker = TimePickerDialog(
                this,
                R.style.PickDialogTheme,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    // Receive the selected hour/minutes and set the time as text to the field
                    val selectedTime = Calendar.getInstance()
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedTime.set(Calendar.MINUTE, minute)
                    btn_end.setText(timeFormat.format(selectedTime.time))
                },
                // Set the hour & minutes appear in the dialog to be the current time
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
            )
            // Show the time picker dialog
            timePicker.show()
        }
    }
}