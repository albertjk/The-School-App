package com.example.schoolapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

/**
 * This class is responsible for displaying items in the RecyclerView.
 */
class NotificationAdapter(var notificationList: List<Notification>) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // These variables are used for debugging.
    private val TAG : String = "MovieAdapter"
    private var count : Int = 0 // This variable is used to count how many times onCreateViewHolder is called.

    /**
     * This function creates the individual rows.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {

        Log.i(TAG, "onCreateViewHolder " + count++)

        // The view of the row.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_row, parent, false)
        return NotificationViewHolder(view)
    }

    /**
     * This function is called for each row and adds the data inside the rows.
     * The position parameter is the row number.
     */
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
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

    /** This class holds the views of a row defined in notification_row.xml.
     *  The itemView parameter is the row.
     */
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        var contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        var dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        var expandableLayout: ConstraintLayout = itemView.findViewById(R.id.expandableLayout)

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
        }
    }
}