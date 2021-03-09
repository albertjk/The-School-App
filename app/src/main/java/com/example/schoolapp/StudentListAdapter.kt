package com.example.schoolapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StudentListAdapter (val students: List<StudentList>, var context:Context)
    : RecyclerView.Adapter<StudentListAdapter.ViewHolder>() , Filterable {
    var filterStudentList: ArrayList<StudentList> = ArrayList()

    // This variable is used to access a Cloud Firestore instance.
    private val db = Firebase.firestore

    init {
        filterStudentList.addAll(students)
        Log.d("TAG","Init List: $filterStudentList")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int{
        Log.d("TAG", "filterNumber: ${filterStudentList.size}")
        return filterStudentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = filterStudentList[position]
        holder.studentNameTextView.text = student.studentName
        holder.studentClassTextView.text = student.studentClass
        holder.studentEmailTextView.text = student.studentEmail
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var studentNameTextView: TextView = itemView.findViewById(R.id.txt_student_Name)
        var studentEmailTextView: TextView = itemView.findViewById(R.id.stu_email)
        var studentClassTextView: TextView = itemView.findViewById(R.id.stu_class)
        var editEvent: ImageButton = itemView.findViewById(R.id.imageButton_Edit)
        var deleteEvent: ImageButton = itemView.findViewById(R.id.imageButton_Delete)

        init {
            var documentid: String = ""
            var isThisStudent: Boolean = false
            deleteEvent.setOnClickListener {
                db.collection("students")
                    .get()
                    .addOnFailureListener { exception ->
                        Log.d("TAG", "Error getting documents: ", exception)
                    }
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            Log.d("TAG", "document ID: $document")

                            // Iterate over each data item about the event
                            for (item in document.data) {
                                if ((item.key == "email") && studentEmailTextView.text == item.value.toString()){
                                    documentid = document.id
                                    Log.d("TAG", "Show the deleted document ID: $documentid")
                                    isThisStudent = true
                                    break
                                }

                            }
                            if(isThisStudent){
                                //build the dialog with a custom theme (defined in styles.xml)
                                val studentsDialog =
                                    AlertDialog.Builder(context, R.style.AlertDialogTheme)

                                //set the title of the dialog
                                studentsDialog.setTitle("Are you sure you want to delete this student?")
                                // Set the cancel button click listener
                                studentsDialog.setNeutralButton("Cancel") { dialog, _ ->
                                    dialog.cancel()
                                }
                                //Set the positive button click listener
                                studentsDialog.setPositiveButton("Delete") { dialog, _ ->

                                    db.collection("students").document(documentid)
                                        .delete()
                                        .addOnSuccessListener {
                                            Log.d("TAG", "DocumentSnapshot successfully deleted!")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.d(
                                                "TAG", "Error deleting document", e)
                                        }
                                        .addOnCompleteListener {
                                            (students as ArrayList<String>).removeAt(
                                                adapterPosition
                                            )
                                            notifyItemRemoved(adapterPosition);
                                        }
                                    dialog.dismiss()
                                }
                                //create the dialog and show it in screen
                                val mDialog = studentsDialog.create()
                                mDialog.show()
                                break
                            }
                        }
                    }
            }
            editEvent.setOnClickListener {

                db.collection("students")
                    .get()
                    .addOnFailureListener { exception ->
                        Log.d("TAG", "Error getting documents: ", exception)
                    }
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            var counter = 0

                            // Iterate over each data item about the event
                            for (item in document.data) {
                                if (item.key == "email" && studentEmailTextView.text == item.value.toString())
                                    counter++
                                if (item.key == "class" && studentClassTextView.text == item.value.toString())
                                    counter++
                            }

                            if (counter == 2) {
                                documentid = document.id

                                Log.d("TAG", "See the context: $context")
                                val intent = Intent(context, EditStudent::class.java)
                                intent.putExtra("documentId", documentid)
                                context.startActivity(intent)
                            }
                        }
                    }
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter(){
            // run on background thread
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                var charString: String = constraint.toString()
                if(charString.isEmpty()){
                    filterStudentList.addAll(students)
                    Log.d("TAG", "EmptyTypingList: $filterStudentList")
                }
                else{
                    var resultList = ArrayList<StudentList>()
                    for(s : StudentList in students){
                        if(s.getName().toLowerCase().contains(charString.toLowerCase())){
                            resultList.add(s)
                            Log.d("TAG", "AddNewList: $resultList")
                        }
                    }
                    filterStudentList = resultList
                }
                var filterResults = FilterResults()
                    filterResults.values = filterStudentList
                return filterResults
            }

            // run on UI thread
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filterStudentList = results?.values as ArrayList<StudentList>
                Log.d("TAG", "FinalFilter: $filterStudentList")
                notifyDataSetChanged()
            }
        }
    }
}