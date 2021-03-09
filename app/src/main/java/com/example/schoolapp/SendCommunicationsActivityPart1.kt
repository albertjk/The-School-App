package com.example.schoolapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*

class SendCommunicationsActivityPart1 : AppCompatActivity() {

    private lateinit var radioGroup : RadioGroup

    // Only one variable is needed for the three radio buttons.
    private lateinit var radioButton : RadioButton

    var EXTRA_SEND_COMMUNICATIONS_SELECTION : String = "com.example.schoolapp.EXTRA_SEND_COMMUNICATIONS_SELECTION"
    var EXTRA_EVENT_MESSAGE: String ="com.example.schoolapp.EXTRA_EVENT_MESSAGE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_communications_part1)

        var nextButton : Button = findViewById(R.id.send_communications_next_button)

        /* If the next button is tapped, check if a radio button is selected.
        If so, redirect to the appropriate activity to type the message. */
        nextButton.setOnClickListener {

            radioGroup = findViewById(R.id.send_communications_radio_group)

            // Get the ID of the selected radio button.
            var radioId : Int = radioGroup.checkedRadioButtonId

            // If a radio button is selected, continue the messaging process. Otherwise, show a Toast to the user.
            if (radioId != - 1) {

                // Find the selected radio button.
                radioButton = findViewById(radioId)

                // Open SendCommunicationsActivityPart2. Send the selected messaging option as a string.
                val intent = Intent(this, SendCommunicationsActivityPart2::class.java).apply {
                    putExtra(EXTRA_SEND_COMMUNICATIONS_SELECTION, radioButton.text)
                    putExtra(EXTRA_EVENT_MESSAGE, intent.getStringExtra(SendCommunicationsActivityPart1().EXTRA_EVENT_MESSAGE))
                }
                startActivity(intent)

            } else {
                Toast.makeText(this, "Please select who you would like to send a message to.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
