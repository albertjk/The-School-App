package com.example.schoolapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

/**
 * This class is responsible for listing student names and e-mail addresses in the RecyclerView.
 */
class StudentAdapter (var studentList: List<Student>, var context : Context, var recipientListener: RecipientSelectionClickListener) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    /**
     * This function creates the individual rows.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        return StudentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_for_option3, parent, false))
    }

    /**
     * This function is called for each row and adds the data inside the rows.
     * The position parameter is the row number.
     */
    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]

        holder.studentNameTextView.text = student.name
        holder.studentEmailTextView.text = student.email

        // Binds a click listener to each row.
        holder.bind(recipientListener)
    }

    /**
     * Returns the number of rows the RecyclerView needs.
     */
    override fun getItemCount(): Int {
        return studentList.size
    }

    /**
     * Sets the filtered list as the new Student object list.
     */
    fun filterList(filteredList : ArrayList<Student>) {
        studentList =  filteredList
        notifyDataSetChanged()
    }

    /**
     * This class holds the views of a row in the RecyclerView defined
     * in activity_send_communications_part2_option3.xml.
     * The itemView parameter is the row of list_item_for_option3.xml.
     */
    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var studentNameTextView: TextView = itemView.findViewById(R.id.student_name_text_view)
        var studentEmailTextView: TextView = itemView.findViewById(R.id.student_email_text_view)

        /**
         * This function is called when an item in the RecyclerView is tapped.
         * It calls the addStudentRecipient function in SendCommunicationsActivityPart2.
         */
        fun bind(recipientListener: RecipientSelectionClickListener) {
            itemView.setOnClickListener {
                val student = studentList[adapterPosition]
                Toast.makeText(context, studentNameTextView.text.toString() + " is added to the recipients" , Toast.LENGTH_SHORT).show()
                recipientListener.addStudentRecipient(student)
                notifyDataSetChanged()
            }
        }
    }
}