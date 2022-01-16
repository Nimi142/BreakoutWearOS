package com.roddyrappaport.breakoutwatch

import android.animation.ValueAnimator
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Scroller
import androidx.core.view.InputDeviceCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewConfigurationCompat



// For touch detection, mostly
class BreakoutGLSurface(context: Context): GLSurfaceView(context) {
    val renderer: BreakoutGLRenderer
    private val TOUCH_SCALE_FACTOR: Float = 3.0f / 320f

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = BreakoutGLRenderer(context)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        setOnGenericMotionListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_SCROLL &&
                ev.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)
            ) {
                // Don't forget the negation here
                val delta = -ev.getAxisValue(MotionEventCompat.AXIS_SCROLL) *
                        ViewConfigurationCompat.getScaledVerticalScrollFactor(
                            ViewConfiguration.get(context), context
                        )
                // Swap these axes to scroll horizontally instead
                renderer.movePeg(delta / 400f)
                true
            } else {
                false
            }
        }
        isFocusableInTouchMode = true
        isFocusable = true
        requestFocus()
    }

    private var previousX: Float = 0f
    private var previousY: Float = 0f

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        val x: Float = e.x
        val y: Float = e.y

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {

                val dx: Float = x - previousX

                renderer.movePeg(dx * TOUCH_SCALE_FACTOR)
                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }


}