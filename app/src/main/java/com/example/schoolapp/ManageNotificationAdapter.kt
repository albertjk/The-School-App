package com.example.schoolapp

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ManageNotificationAdapter(var notificationList: List<Notification>,var context: Context) : RecyclerView.Adapter<ManageNotificationAdapter.ManageNotificationViewHolder>() {

    // This variable is used to access a Cloud Firestore instance
    private val db = Firebase.firestore

    /**
     * Creates every notification row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageNotificationViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.manage_notification_row, parent, false)
        return ManageNotificationViewHolder(view)
    }

    /**
     * Adds every data in the row.
     */
    override fun onBindViewHolder(holder: ManageNotificationViewHolder, position: Int) {
        val notification = notificationList[position]
        holder.titleTextView.text = notification.title
        holder.contentTextView.text = notification.content
        holder.dateTextView.text = notification.date.toString()

        // Store whether the current row is expanded or not.
        val isExpanded: Boolean = notificationList[position].isItemExpanded

        // Set the visibility based on whether the row is expanded or not.
        holder.expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    /**
     * Returns the number of rows the RecyclerView needs.
     */
    override fun getItemCount(): Int {
        return notificationList.size
    }

    /**
     * Binds data to the textView in every row.
     */
    inner class ManageNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        var contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        var dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        var expandableLayout: ConstraintLayout = itemView.findViewById(R.id.expandableLayout)
        var deleteNotification: ImageButton = itemView.findViewById(R.id.Notification_Delete)

        // Initializer block
        init {
            // Click listener for each individual row. If a row is tapped ...
            titleTextView.setOnClickListener {
                /* ... get the notification at this adapter position
                and expand it if it is not expanded and vice versa. */
                val notification = notificationList[adapterPosition]
                notification.isItemExpanded = (!notification.isItemExpanded)
                notifyItemChanged(adapterPosition)
            }

            var documentid: String = ""

            deleteNotification.setOnClickListener {

                db.collection("notifications")
                    .get()
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            var counter = 0

                            // Iterate over each data item about the notification
                            for (item in document.data) {
                                if ((item.key == "title") && titleTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "date" && dateTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "content" && contentTextView.text == item.value.toString())
                                    counter++

                            }

                            Log.d("counter", counter.toString())
                            if (counter == 2) {
                                documentid = document.id

                                //build the dialog with a custom theme (defined in styles.xml)
                                val studentsDialog =
                                    AlertDialog.Builder(context, R.style.AlertDialogTheme)

                                //set the title of the dialog
                                studentsDialog.setTitle("Are you sure you want to delete this notification?")
                                // Set the cancel button click listener
                                studentsDialog.setNeutralButton("Cancel") { dialog, _ ->
                                    dialog.cancel()
                                }
                                //Set the positive button click listener
                                studentsDialog.setPositiveButton("Delete") { dialog, _ ->

                                    db.collection("notifications").document(documentid)
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
                                            (notificationList as ArrayList<String>).removeAt(
                                                adapterPosition
                                            )
                                            notifyItemRemoved(adapterPosition);
                                        }

                                    dialog.dismiss()
                                }

                                //create the dialog and show it in screen
                                val mDialog = studentsDialog.create()
                                mDialog.show()
                            }
                        }
                    }
            }
        }
    }
}