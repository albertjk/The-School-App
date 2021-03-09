package com.example.schoolapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

/**
 * This class is responsible for listing class IDs in the RecyclerView.
 */
class SchoolClassAdapter (var schoolClassList: List<SchoolClass>, var context : Context, var recipientListener: RecipientSelectionClickListener) : RecyclerView.Adapter<SchoolClassAdapter.SchoolClassViewHolder>() {

    /**
     * This function creates the individual rows.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchoolClassViewHolder {
        return SchoolClassViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_for_option2, parent, false))
    }

    /**
     * This function is called for each row and adds the data inside the rows.
     * The position parameter is the row number.
     */
    override fun onBindViewHolder(holder: SchoolClassViewHolder, position: Int) {
        val schoolClassItem = schoolClassList[position]

        holder.classIdTextView.text = schoolClassItem.classID

        // Binds a click listener for each row.
        holder.bind(recipientListener)
    }

    /**
     * Returns the number of rows the RecyclerView needs.
     */
    override fun getItemCount(): Int {
        return schoolClassList.size
    }

    /**
     * Sets the filtered list as the new school class list.
     */
    fun filterList(filteredList : ArrayList<SchoolClass>) {
        schoolClassList =  filteredList
        notifyDataSetChanged()
    }

    /**
     * This class holds the views of a row in the RecyclerView defined
     * in activity_send_communications_part2_option2.xml.
     * The itemView parameter is the row of list_item_for_option2.xml.
     */
    inner class SchoolClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var classIdTextView: TextView = itemView.findViewById(R.id.class_id_text_view)

        /**
         * This function is called when an item in the RecyclerView is tapped.
         * It calls the addClassRecipient function in SendCommunicationsActivityPart2.
         */
        fun bind(recipientListener: RecipientSelectionClickListener) {
            itemView.setOnClickListener {
                val schoolClass = schoolClassList[adapterPosition]
                Toast.makeText(context, schoolClass.classID + " is added to the recipients" , Toast.LENGTH_SHORT).show()
                recipientListener.addClassRecipient(schoolClass.classID)
                notifyDataSetChanged()
            }
        }
    }
}