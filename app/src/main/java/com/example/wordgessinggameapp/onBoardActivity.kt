package com.example.wordgessinggameapp

import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_board)

        val UserNameEditText = findViewById<EditText>(R.id.UserNameEditText)
        val SubmitButton = findViewById<Button>(R.id.SubmitButton)

        SubmitButton.setOnClickListener {
            val userName = UserNameEditText.text.toString().trim()
            if (userName.isNotEmpty()) {
                saveUserName(this, userName)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserName(context: Context, userName: String) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userName", userName)
        editor.apply()
    }


}
