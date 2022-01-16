package com.roddyrappaport.breakoutwatch

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.wear.widget.BoxInsetLayout

class BreakoutActivity : Activity() {
    private lateinit var glView: GLSurfaceView
    private lateinit var scoreView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Creating constraint Layout
        val boxLayout = BoxInsetLayout(this)
        boxLayout.setBackgroundColor(getColor(R.color.faint_red))
        boxLayout.id = View.generateViewId()
        setContentView(boxLayout)
        val layout = ConstraintLayout(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            layout.layoutParams = BoxInsetLayout.LayoutParams(BoxInsetLayout.LayoutParams.MATCH_PARENT, BoxInsetLayout.LayoutParams.MATCH_PARENT, BoxInsetLayout.LayoutParams.UNSPECIFIED_GRAVITY, BoxInsetLayout.LayoutParams.BOX_ALL)
        }
        else {
            // I hope the zero doesn't do anything. I don't use gravity so it really shouldn't matter
            layout.layoutParams = BoxInsetLayout.LayoutParams(BoxInsetLayout.LayoutParams.MATCH_PARENT, BoxInsetLayout.LayoutParams.MATCH_PARENT, 0, BoxInsetLayout.LayoutParams.BOX_ALL)
        }
        layout.id = View.generateViewId()
        // Creating Score and Game views
        glView = BreakoutGLSurface(this)
        scoreView = TextView(this)

        // Generating IDs
        scoreView.id = View.generateViewId()
        glView.id = View.generateViewId()

        // Initializing scoreView
        scoreView.text = getString(R.string.score_string, 0)


        // Adding Views to layout
        layout.addView(glView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        layout.addView(scoreView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        // Constraining views to layout
        val set = ConstraintSet()
        set.clone(layout)
        set.connect(scoreView.id, ConstraintSet.BOTTOM, layout.id, ConstraintSet.BOTTOM)
        set.connect(scoreView.id, ConstraintSet.LEFT, layout.id, ConstraintSet.LEFT)
        set.applyTo(layout)
        boxLayout.addView(layout)
    }

    fun updateScore(newScore: Int) {
        runOnUiThread {
            scoreView.text = getString(R.string.score_string, newScore)
        }
        val sharedPref = getSharedPreferences("highscores", Context.MODE_PRIVATE)
        if (newScore > sharedPref.getInt("score", 0)) {
            with (sharedPref.edit()) {
                putInt("score", newScore)
                apply()
            }
        }
    }

    // If full clear it saves fastest time (seconds)
    fun updateTime(newTime: Long) {
        val sharedPref = getSharedPreferences("highscores", Context.MODE_PRIVATE)
        val prevLowestTime = sharedPref.getLong("time", -1)
        if (prevLowestTime == -1L || newTime < prevLowestTime) {
            with (sharedPref.edit()) {
                putLong("time", newTime)
                apply()
            }
        }
    }
}