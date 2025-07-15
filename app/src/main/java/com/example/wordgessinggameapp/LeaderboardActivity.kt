package com.example.wordgessinggameapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import org.json.JSONObject
import android.content.Intent

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var backToMainButton : Button
    private lateinit var leaderboardListView: ListView
    private val leaderboardScores = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        leaderboardListView = findViewById(R.id.leaderboardListView)

        leaderboardListView = findViewById(R.id.leaderboardListView)

        backToMainButton = findViewById(R.id.backToMainButton)


        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // This will finish LeaderboardActivity and remove it from the back stack
        }

        fetchLeaderboardScores() // Fetch leaderboard scores when the activity is created
    }


    private fun fetchLeaderboardScores() {
        val dreamloAPI = "http://dreamlo.com/lb/6713cff48f40bc122c2803b1/json"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(dreamloAPI)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LeaderboardActivity, "Failed to fetch leaderboard", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""

                    try {
                        val leaderboardObject = JSONObject(responseBody).getJSONObject("dreamlo")
                        // Check if the leaderboard exists and is not null
                        if (!leaderboardObject.isNull("leaderboard")) {
                            val leaderboardData = leaderboardObject.getJSONArray("leaderboard")

                            leaderboardScores.clear()
                            for (i in 0 until leaderboardData.length()) {
                                val entry = leaderboardData.getJSONObject(i)
                                val playername = entry.getString("name")
                                val score = entry.getInt("score")
                                leaderboardScores.add("$playername: $score")
                            }

                            runOnUiThread {
                                displayLeaderboard()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@LeaderboardActivity, "Leaderboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            Toast.makeText(this@LeaderboardActivity, "Error parsing leaderboard data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }




    private fun displayLeaderboard() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, leaderboardScores)
        leaderboardListView.adapter = adapter
    }
}