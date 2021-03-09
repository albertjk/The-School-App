package com.example.schoolapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast

class SignUpActivityPart1 : AppCompatActivity() {

    private lateinit var radioGroup : RadioGroup

    // Only one variable is needed for the two radio buttons.
    private lateinit var radioButton : RadioButton

    val EXTRA_SIGN_UP_SELECTION : String = "com.example.schoolapp.EXTRA_SIGN_UP_SELECTION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_part1)

        var nextButton : Button = findViewById(R.id.sign_up_next_button)

        /* If the next button is clicked, check if a radio button is selected.
        If so, redirect to the appropriate sign up. */
        nextButton.setOnClickListener {

            radioGroup = findViewById(R.id.sign_up_radio_group)

            // Get the ID of the selected radio button.
            var radioId : Int = radioGroup.checkedRadioButtonId

            // If a radio button is selected, continue the sign up process. Otherwise, show a Toast to the user.
            if (radioId != - 1) {

                // Find the selected radio button.
                radioButton = findViewById(radioId)

                // Open SignUpActivityPart2. Send the selected type of profile to be created as a string: "Student" or "Parent".
                val intent = Intent(this, SignUpActivityPart2::class.java).apply {
                    putExtra(EXTRA_SIGN_UP_SELECTION, radioButton.text)
                }
                startActivity(intent)

            } else {
                Toast.makeText(this, "Please select a type of profile to sign up for.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
