package com.roddyrappaport.breakoutwatch

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.roddyrappaport.breakoutwatch.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateHighscore()
    }

    override fun onResume() {
        super.onResume()
        updateHighscore()
    }

    private fun updateHighscore() {
        val lowestClear: Long =  getSharedPreferences("highscores", Context.MODE_PRIVATE).getLong("time", -1L)
        val scoreView = findViewById<TextView>(R.id.HighscoreView)
        if (lowestClear == -1L) {
            scoreView.text = getString(R.string.score_string, getSharedPreferences("highscores", Context.MODE_PRIVATE).getInt("score", 0))
        }
        else {
            scoreView.text = getString(R.string.full_clear_time_string, lowestClear)
        }
    }


    fun playButton(v: View) {
        startActivity(Intent(this, BreakoutActivity::class.java))
    }
}