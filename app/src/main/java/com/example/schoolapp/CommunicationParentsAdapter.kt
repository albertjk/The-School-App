package com.example.schoolapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

/**
 * This class is responsible for displaying items in the RecyclerView.
 */
class CommunicationParentsAdapter(
    var communicationParentsList: List<CommunicationParents>,
    var intent: Context
) : RecyclerView.Adapter<CommunicationParentsAdapter.CommunicationViewHolder>() {

    // This variable ise used for debugging.
    private val TAG = CommunicationParentsAdapter::class.qualifiedName

    // This variable is used to count how many times onCreateViewHolder is called.
    private var count : Int = 0

    // Access a Cloud Firestore instance
    private val db = Firebase.firestore

    /**
     * This function creates the individual rows.
     */
    @SuppressLint("LongLogTag")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunicationViewHolder {
        // The view of the row.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.communicationparents_row, parent, false)
        return CommunicationViewHolder(view)
    }

    /**
     * This function is called for each row and adds the data inside the rows.
     * The position parameter is the row number.
     */
    override fun onBindViewHolder(holder: CommunicationViewHolder, position: Int) {

        // Get the specific communication from the list and its details
        val communication = communicationParentsList[position]
        holder.titleTextView.text = communication.title
        holder.contentTextView.text = communication.content
        holder.dateTextView.text = communication.date.toString()

        // Store whether the current row is expanded or not.
        val isExpanded: Boolean = communicationParentsList[position].isItemExpanded

        // Set the visibility based on whether the row is expanded or not.
        holder.expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    /**
     * Returns the number of rows the RecyclerView needs.
     */
    override fun getItemCount(): Int {
        return communicationParentsList.size
    }

    /**
     * This class holds the views of a row defined in communicationparents_row.xml.
     * The itemView parameter is the row.
     */
    @SuppressLint("LongLogTag")
    inner class CommunicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //get the ids of the fields from the layout
        var titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        var contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        var dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        var recipientsButton: Button = itemView.findViewById(R.id.btn_recipients)
        var expandableLayout: ConstraintLayout = itemView.findViewById(R.id.expandableLayout)

        // Initialiser block
             init {

            // Click listener for each individual row. If a row is tapped ...
            titleTextView.setOnClickListener {
                /* ... get the notification at this adapter position
                and expand it if it is not expanded and vice versa. */
                val communication = communicationParentsList[adapterPosition]
                communication.isItemExpanded = (!communication.isItemExpanded)
                notifyItemChanged(adapterPosition)
            }

            // Functionality when the user clicks on the button to view all the recipients
            recipientsButton.setOnClickListener{

                // Get the corresponding communication object
                val communication = communicationParentsList[adapterPosition]

                // Initialise the list for the recipients
                var listNames:ArrayList<String> = ArrayList()
                db.collection("communications")
                    .get()
                    .addOnSuccessListener { documents ->

                        // For each communication
                        for (document in documents) {

                            // If the fields matches the current communication
                            var date= Date()
                            var timestamp = document.data["Date"] as Timestamp?
                            if (timestamp != null) {
                                date = timestamp.toDate()
                            }
                                if (communication.content==document.data["Message"].toString()
                                    && communication.title==document.data["Subject"].toString()
                                    && communication.date.toString()==date.toString()){
                                    var counter=0

                                    // For each recipient
                                    for (rec in document.data["To"] as ArrayList<String>) {
                                        var name = ""
                                        db.collection("students")
                                            .whereEqualTo("email", rec)
                                            .get()

                                            // If the e-mail/username is found, also find the stored password.
                                            .addOnSuccessListener { documents ->
                                                for (document in documents) {
                                                    name ="" + document.data["first name"] + " " + document.data["last Name"]

                                                    // Add the recipient to the list
                                                    listNames.add(name)
                                                    counter = counter + 1
                                                }
                                            }
                                            .addOnFailureListener { exception ->
                                                Log.d(TAG, "get failed with ", exception)
                                            }
                                            .addOnCompleteListener {

                                                db.collection("parents")
                                                    .whereEqualTo("username", rec)
                                                    .get()

                                                    // If the e-mail/username is found, also find the stored password.
                                                    .addOnSuccessListener { documents ->
                                                        for (document in documents) {

                                                            name =
                                                                "" + document.data["first name"] + " " + document.data["last name"]

                                                            // Add the recipient to the lsit
                                                            listNames.add(name)
                                                            counter = counter + 1
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        if (counter == (document.data["To"] as ArrayList<String>).size) {
                                                            counter=0
                                                            val studentsDialog =
                                                                AlertDialog.Builder(
                                                                    intent,
                                                                    R.style.AlertDialogTheme
                                                                )
                                                            // Set the title of the dialog
                                                            studentsDialog.setTitle("Recipients of the message")
                                                            studentsDialog.setMessage(listNames.toString().replace("[","").replace("]","").trim())
                                                            // Set the cancel button click listener
                                                            studentsDialog.setNeutralButton("Cancel") { dialog, _ ->
                                                                dialog.cancel()
                                                            }
                                                            // Create the dialog and show it in screen
                                                            val mDialog = studentsDialog.create()
                                                            mDialog.show()
                                                        }
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Log.d(TAG, "get failed with ", exception)
                                                    }
                                            }
                                    }
                                }
                            }
                        }

                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }
            }
        }
    }
}