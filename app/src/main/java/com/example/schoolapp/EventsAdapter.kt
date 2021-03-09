package com.example.schoolapp
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

/**
 * This class is responsible for displaying items in the RecyclerView.
 */
class EventsAdapter(var eventList: List<Events>, var context:Context, var currentUser:String) : RecyclerView.Adapter<EventsAdapter.EventsViewHolder>() {

    //variable that indicates whether the event will be send as a communication
    var EXTRA_EVENT_MESSAGE: String ="com.example.schoolapp.EXTRA_EVENT_MESSAGE"

    // These variables are used for debugging.
    private val TAG = EventsAdapter::class.qualifiedName

    // This variable is used to count how many times onCreateViewHolder is called.
    private var count: Int = 0

    // Access a Cloud Firestore instance
    private val db = Firebase.firestore

    /**
     * This function creates the individual rows.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsViewHolder {

        // The view of the row.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.events_row, parent, false)

        //find the ids of the buttons with which Admin can manage the events
        var editEvent:ImageButton = view.findViewById(R.id.imageButton_Edit)
        var sendEvent:ImageButton = view.findViewById(R.id.imageButton_Send)
        var deleteEvent:ImageButton = view.findViewById(R.id.imageButton_Delete)

        //if the user is the admin make visible the buttons with which he can manage the events
        if (currentUser=="0") {
            editEvent.visibility = View.VISIBLE
            sendEvent.visibility = View.VISIBLE
            deleteEvent.visibility= View.VISIBLE
        }
        //otherwise hide them.
        else {
            editEvent.visibility = View.INVISIBLE
            sendEvent.visibility = View.INVISIBLE
            deleteEvent.visibility= View.INVISIBLE
        }
        return EventsViewHolder(view)
    }

    /**
     * This function is called for each row and adds the data inside the rows.
     * The position parameter is the row number.
     */
    override fun onBindViewHolder(holder: EventsViewHolder, position: Int) {

        //for a specific event, receive its information
        val event = eventList[position]
        holder.titleTextView.text = event.title
        holder.dateTextView.text = event.date.toString()
        holder.descriptionTextView.text = event.description
        holder.enddateTextView.text = event.endDate
        holder.startdateTextView.text = event.startDate
        holder.locationTextView.text = event.location
        holder.shortDescriptionTextView.text=event.shortDescription
        holder.coordinatesTextView.text= event.coordinates.toString()

        // Store whether the current row is expanded or not.
        val isExpanded: Boolean = eventList[position].isItemExpanded

        //if user wants to see the location of the event in maps, he clicks to the correspond button, so the LocationMaps activity starts
        holder.locationButton.setOnClickListener(){
            if (event.coordinates?.latitude != 0.0) {
                val intent = Intent(context, LocationMapsActivity::class.java)
                //send the coordinates of the event location in order to be find in the map
                intent.putExtra("coordinates1", event.coordinates?.latitude.toString())
                intent.putExtra("coordinates2", event.coordinates?.longitude.toString())
                intent.putExtra("currentUser",currentUser.toString())
                context.startActivity(intent)
            }
            else{
                //if there are no coordinates inform the user
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Message")
                builder.setMessage("No specified coordinates for the specific location.")
                builder.setNeutralButton("Okay") { dialog, which ->
                    Toast.makeText(context,
                        "Okay", Toast.LENGTH_SHORT).show()
                }
                builder.show()
            }
        }
        // Set the visibility based on whether the row is expanded or not.
        holder.expandableLayout.visibility = if (isExpanded) {
            View.VISIBLE
        } else View.GONE
    }

    /**
     * Returns the number of rows the RecyclerView needs.
     */
    override fun getItemCount(): Int {
        return eventList.size
    }

    /** This class holds the views of a row defined in communicationparents_row.xml.
     * The itemView parameter is the row.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    inner class EventsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        //receive the ids for each field in the event row
        var titleTextView: TextView = itemView.findViewById(R.id.txt_title)
        var dateTextView: TextView = itemView.findViewById(R.id.txt_date)
        var startdateTextView: TextView = itemView.findViewById(R.id.txt_start_date)
        var enddateTextView: TextView = itemView.findViewById(R.id.txt_end_date)
        var descriptionTextView: TextView = itemView.findViewById(R.id.txt_description)
        var locationTextView: TextView = itemView.findViewById(R.id.txt_location)
        var shortDescriptionTextView: TextView = itemView.findViewById(R.id.txt_shortDescription)
        var locationButton: Button = itemView.findViewById(R.id.button_location)
        var coordinatesTextView: TextView = itemView.findViewById(R.id.txt_coordinates)
        var expandableLayout: ConstraintLayout = itemView.findViewById(R.id.expandableLayout)
        var editEvent:ImageButton = itemView.findViewById(R.id.imageButton_Edit)
        var sendEvent:ImageButton = itemView.findViewById(R.id.imageButton_Send)
        var deleteEvent:ImageButton = itemView.findViewById(R.id.imageButton_Delete)

        // Initializer block
        init {
            // Click listener for each individual row. If a row is tapped ...
            titleTextView.setOnClickListener {
                /* ... get the event at this adapter position
                and expand it if it is not expanded and vice versa. */
                val event = eventList[adapterPosition]
                event.isItemExpanded = (!event.isItemExpanded)
                notifyItemChanged(adapterPosition)
            }
            var documentid: String = ""
            var date: Date?=null

            //if the delete button is pressed, then the event is found in the database and is deleted, while the list is updated in real-time
            deleteEvent.setOnClickListener {
                db.collection("events")
                    .get()
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            var counter = 0

                            // Iterate over each data item about the event
                            for (item in document.data) {
                                if ((item.key == "Title") && titleTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "Date") {
                                    var timestamp = item.value as Timestamp?
                                    counter++
                                }
                                if (item.key == "End" && enddateTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "Start" && startdateTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "ShortDescription" && shortDescriptionTextView.text == item.value.toString())
                                    counter++

                                if (item.key == "Content" && descriptionTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "Location" && locationTextView.text == item.value.toString())
                                    counter++
                            }
                            var temp: String =
                                "GeoPoint { latitude=" + (document.data["Coordinates"] as GeoPoint).latitude + ", longitude=" + ((document.data["Coordinates"] as GeoPoint).longitude) + " }"
                            if (temp == (document.data["Coordinates"].toString()))
                                counter++

                            /* if all the fields in the event row match the correspond fields in an event in DB
                            receive its document id */
                            if (counter == 8) {
                                documentid = document.id

                                // Build the dialog with a custom theme (defined in styles.xml)
                                val studentsDialog =
                                    AlertDialog.Builder(context, R.style.AlertDialogTheme)

                                // Set the title of the dialog
                                studentsDialog.setTitle("Are you sure you want to delete this event?")

                                // Set the cancel button click listener
                                studentsDialog.setNeutralButton("Cancel") { dialog, _ ->
                                    dialog.cancel()
                                }

                                // Set the positive button click listener
                                studentsDialog.setPositiveButton("Delete") { dialog, _ ->
                                    db.collection("events").document(documentid)
                                        .delete()
                                        .addOnSuccessListener {
                                            Log.d(
                                                TAG,
                                                "DocumentSnapshot successfully deleted!"
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(
                                                TAG,
                                                "Error deleting document",
                                                e
                                            )
                                        }
                                        .addOnCompleteListener {
                                            (eventList as ArrayList<String>).removeAt(
                                                adapterPosition
                                            )
                                            notifyItemRemoved(adapterPosition);
                                        }
                                    dialog.dismiss()
                                }

                                // Create the dialog and show it in screen
                                val mDialog = studentsDialog.create()
                                mDialog.show()
                            }
                        }
                    }
            }

            /* If the edit button next to an event is pressed, then the admin can edit the event,
            so the activity AddNewEvent is started but it will show the corresponding data of the event in the fields. */
            editEvent.setOnClickListener {
                db.collection("events")
                    .get()
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            var counter = 0
                            // Iterate over each data item about the event
                            for (item in document.data) {
                                if ((item.key == "Title") && titleTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "Date") {
                                    var timestamp = item.value as Timestamp?
                                    counter++
                                }
                                if (item.key == "End" && enddateTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "Start" && startdateTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "ShortDescription" && shortDescriptionTextView.text == item.value.toString())
                                    counter++

                                if (item.key == "Content" && descriptionTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "Location" && locationTextView.text == item.value.toString())
                                    counter++

                            }
                            var temp:String="GeoPoint { latitude=" +(document.data["Coordinates"] as GeoPoint).latitude +", longitude="+((document.data["Coordinates"] as GeoPoint).longitude)+" }"
                            if (temp == (document.data["Coordinates"].toString()))
                                counter++

                            /* If all the fields in the event row match the corresponding fields in an event in DB
                            receive its document id. */
                            if (counter == 8) {
                                documentid = document.id

                                /* Start the activity related to the addition of a new event and
                                send the document ID of the event that is desired to be edited. */
                                val intent = Intent(context, AddNewEventActivity::class.java)
                                intent.putExtra("documentId", documentid)
                                context.startActivity(intent)
                            }
                        }
                    }
            }
            /* If the send event icon is clicked, then the admin wants to send the event to
            students & parents, so the SendCommunicationsActivityPart1 is started with a custom message as content. */
            sendEvent.setOnClickListener{
                var content="Hello! You can check the new event \"${titleTextView.text}\" located in ${locationTextView.text} on ${dateTextView.text}."
                val intent = Intent(context, SendCommunicationsActivityPart1::class.java)

                // Send the content of the message
                intent.putExtra(EXTRA_EVENT_MESSAGE, content)
                context.startActivity(intent)
            }
        }
    }
}