package com.example.wordgessinggameapp

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import org.json.JSONArray
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    private lateinit var timerTextView: TextView
    private lateinit var guessEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var attemptTextView: TextView
    private lateinit var scoreTextView: TextView


    private lateinit var userNameTextView: TextView
    private lateinit var getClueButton: Button
    private lateinit var checkLetterButton: Button
    private lateinit var checkLengthButton: Button
    private lateinit var exitButton: Button

    private lateinit var submitLeaderboardButton: Button
    private lateinit var leaderboardMessageTextView: TextView

    private var attempts = 0
    private var maxAttempts = 10
    private var secretWord = ""
    private var remainingGuesses = 10
    private var score = 100
    private val handler = Handler(Looper.getMainLooper())
    private var timeInSeconds = 0
    private var timerRunning = false
    private var startTime = 0L
    private val rhymingWordsList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        timerTextView = findViewById(R.id.timerTextView)
        guessEditText = findViewById(R.id.guessEditText)
        submitButton = findViewById(R.id.submitButton)
        attemptTextView = findViewById(R.id.attemptTextView)
        scoreTextView = findViewById(R.id.scoreTextView)
        userNameTextView = findViewById(R.id.userNameTextView)
        getClueButton = findViewById(R.id.getClueButton)
        checkLetterButton = findViewById(R.id.checkLetterButton)
        checkLengthButton = findViewById(R.id.checkLengthButton)
        exitButton = findViewById(R.id.exitButton)

        submitLeaderboardButton = findViewById(R.id.submitLeaderboardButton)
        leaderboardMessageTextView = findViewById(R.id.leaderboardMessageTextView)



        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", null)


        //enter username after installation
        if (userName == null) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()

        } else {
            userNameTextView.text = "Welcome, $userName!"
        }

        startTimer()
        startNewGame()



        // submit guessing word
        submitButton.setOnClickListener {
            val guess = guessEditText.text.toString().trim()
            if (guess.isNotEmpty()) {
                val cleanGuess = guess.replace("[", "").replace("]", "").replace("\"", "")
                guessWord(cleanGuess)
            } else {
                Toast.makeText(this, "Please enter a guess!", Toast.LENGTH_SHORT).show()
            }
        }

        // function for exit from the app
        exitButton.setOnClickListener {
            finish()
        }


        // submit score into leaderboard
        submitLeaderboardButton.setOnClickListener {
            submitToLeaderboard(score)
        }

        // get clue/tip clicking get tip button
        getClueButton.setOnClickListener {
            if (attempts >= 5) {
                getTip()
            } else {
                Toast.makeText(
                    this,
                    "Need at least 5 incorrect guesses to get a tip.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // check letter length
        checkLetterButton.setOnClickListener {
            val letter = guessEditText.text.toString().trim()
            if (letter.length == 1) {
                val occurrence = checkLetterOccurrence(letter[0])
                Toast.makeText(this, "$occurrence occurrences of '$letter'", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Please enter a single letter.", Toast.LENGTH_SHORT).show()
            }
        }

        checkLengthButton.setOnClickListener {
            val length = checkWordLength()
            Toast.makeText(this, "The secret word has $length letters.", Toast.LENGTH_SHORT).show()
        }


    }


    // new  game start function
    private fun startNewGame() {
        fetchRandomWord { randomWord ->
            secretWord = randomWord
            remainingGuesses = maxAttempts
            score = 100
            attempts = 0

            updateScore()
        }
    }

    // timer function
    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerRunning = true
        handler.postDelayed(timerRunnable, 1000)
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (timerRunning) {
                timeInSeconds++
                val minutes = timeInSeconds / 60
                val seconds = timeInSeconds % 60
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
    }

    // stop timer
    private fun stopTimer() {
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    // guess word function
    private fun guessWord(guess: String) {
        if (remainingGuesses > 0) {
            attempts++
            attemptTextView.text = "Attempts: $attempts"
            remainingGuesses--

            if (guess.equals(secretWord, true)) {
                stopTimer()
                val timeElapsed = (System.currentTimeMillis() - startTime) / 1000
                Toast.makeText(this, "Correct! Time: $timeElapsed seconds", Toast.LENGTH_SHORT)
                    .show()
                submitToLeaderboard(score)
                startNewGame()

            } else {
                score -= 10
                updateScore()
                if (remainingGuesses <= 0) {
                    Toast.makeText(
                        this,
                        "Game over! The word is '$secretWord'.",
                        Toast.LENGTH_SHORT
                    ).show()
                    startNewGame()
                } else {
                    Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // word occurrence letters
    private fun checkLetterOccurrence(letter: Char): Int {
        score -= 5
        updateScore()
        return secretWord.count { it.equals(letter, true) }
    }

    //word length
    private fun checkWordLength(): Int {
        score -= 5
        updateScore()
        return secretWord.length
    }


    // get tip
    private fun getTip() {
        score -= 5
        updateScore()

        // API checking
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.api-ninjas.com/v1/rhyme?word=$secretWord")
            .addHeader("X-Api-Key", "ApAWFZ4UiFb9qzo8oB4tqbD25TYuOeCZfjbyIHva")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch tips", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {

                    val jsonResponse = response.body?.string() ?: ""
                    val jsonArray = JSONArray(jsonResponse)

                    if (jsonArray.length() > 0) {

                        val rhymes = mutableListOf<String>()
                        //for (i in 0 until jsonArray.length())

                        val rhyme = jsonArray.getString(0)
                            .replace("[", "").replace("]", "").replace("\"", "")
                        rhymes.add(rhyme)



                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Rhyming words: ${rhymes.joinToString(", ")}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "No rhymes found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }


    // API random words
    private fun fetchRandomWord(callback: (String) -> Unit) {
        val apiUrl = "https://api.api-ninjas.com/v1/randomword"
        val apiKey = "ApAWFZ4UiFb9qzo8oB4tqbD25TYuOeCZfjbyIHva"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("X-Api-Key", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch word", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string() ?: ""
                    val randomWord = JSONObject(jsonResponse).getString("word")
                        .replace("[", "").replace("]", "").replace("\"", "")
                    runOnUiThread {
                        callback(randomWord)
                    }
                }
            }
        })
    }

    private fun updateScore() {
        if (score <= 0) {
            scoreTextView.text = "Score: 0"
            Toast.makeText(this, "Game over! Your score has reached zero and correct word is '$secretWord' ", Toast.LENGTH_SHORT)
                .show()
        } else {
            scoreTextView.text = "Score: $score"
        }
    }


    // submit leaderboard
    private fun submitToLeaderboard(score: Int) {
        val playerName = userNameTextView.text.toString().replace("Welcome, ", "")
        //val encodedPlayerName = URLEncoder.encode(playerName, "UTF-8") // Encode the username for safe URL usage
        val dreamloAPI = "http://dreamlo.com/lb/ZzJm4Uctr0mKNdv_Z66UogKnFkVKCEVEmMVAbzGsXnEQ/add/$playerName/$score"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(dreamloAPI)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to submit score: ${e.message}", Toast.LENGTH_SHORT).show()
                    leaderboardMessageTextView.text = "Failed to submit score. Please try again."
                    leaderboardMessageTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    leaderboardMessageTextView.visibility = TextView.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Score submitted successfully!", Toast.LENGTH_SHORT).show()
                        leaderboardMessageTextView.text = "Score submitted successfully!"
                        leaderboardMessageTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        leaderboardMessageTextView.visibility = TextView.VISIBLE

                        // Delay navigation to leaderboard screen by a couple of seconds
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@MainActivity, LeaderboardActivity::class.java)
                            startActivity(intent)
                        }, 2000) // Delay of 2 seconds before navigating
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to submit score: ${response.message}", Toast.LENGTH_SHORT).show()
                        leaderboardMessageTextView.text = "Failed to submit score. Please try again."
                        leaderboardMessageTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                        leaderboardMessageTextView.visibility = TextView.VISIBLE
                    }
                }
            }
        })
    }

}


