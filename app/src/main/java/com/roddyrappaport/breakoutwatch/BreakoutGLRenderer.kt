package com.roddyrappaport.breakoutwatch

import android.content.Context
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.absoluteValue

class BreakoutGLRenderer(val context: Context): GLSurfaceView.Renderer {
    private var height: Int = 0
    private var width: Int = 0


    var gameStartTime: Long = 0L
    
    private lateinit var ballCircle: GLCircleSprite
    private lateinit var pegSquare: GLSquareSprite

    private var brickSquares: MutableList<GLSquareSprite> = mutableListOf()
    private var destroyedBrickSquares: MutableList<GLSquareSprite> = mutableListOf()

    private var brokenRed = false
    private var brokenOrange = false
    private var bricksBroken = 0

    private var circleMoveTime: Long = 0

    private fun restart() {
        brokenRed = false
        brokenOrange = false
        bricksBroken = 0
        updateScore(bricksBroken)
        ballCircle.radius = 0.05f
        ballCircle.setVelocity(0f, -1f)
        ballCircle.speed = 0.015f
        ballCircle.setVelocity(ballCircle.velocity[0], ballCircle.velocity[1])
        ballCircle.centerX = 0f
        ballCircle.centerY = 0f
        pegSquare.rectF = RectF(-0.2f, -0.6f, 0.2f, -0.7f)
        for (i in destroyedBrickSquares.iterator()) {
            brickSquares.add(i)
        }
        destroyedBrickSquares = mutableListOf()
        gameStartTime = System.currentTimeMillis()
    }

    private fun updateScore(newScore: Int) {
        // Save if possible, and show new score
        val breakoutActivity = (context as? BreakoutActivity) ?: return
        breakoutActivity.updateScore(newScore)
    }

    private fun updateTime(newTime: Long) {
        // Save if possible, and show new score
        val breakoutActivity = (context as? BreakoutActivity) ?: return
        breakoutActivity.updateTime(newTime)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        pegSquare = GLSquareSprite(floatArrayOf(1f, 1f, 1f, 1f))

        ballCircle = GLCircleSprite(floatArrayOf(1f, 1f, 1f, 1f))


        // Creating Bricks
        val numInLine = 14
        val brickXSize = 2f / numInLine
        val brickYSize = 0.4f / 8
        for (i in 0..7) {
            val color = when (i) {
                0, 1 -> floatArrayOf(1f, 0f, 0f, 1f)  // Red
                2, 3 -> floatArrayOf(1f, 165 / 255f, 0f, 1f)  // Orange
                4, 5 -> floatArrayOf(0f, 1f, 0f, 1f)  // Green
                6, 7    -> floatArrayOf(1f, 1f, 0f, 1f)  // Yellow
                else -> floatArrayOf(1f, 1f, 1f, 1f)  // Error, WHITE
            }
            for (j in 0 until numInLine) {
                val newSquare = GLSquareSprite(color)
                newSquare.rectF = RectF(-1 + j * brickXSize, 1 - i * brickYSize, -1 + (j + 1) * brickXSize, 1 - (i + 1) * brickYSize)
                brickSquares.add(newSquare)
            }
        }
        restart()
    }

    fun increaseBallSpeed() {
        ballCircle.speed += 0.01f
        ballCircle.setVelocity(ballCircle.velocity[0], ballCircle.velocity[1])
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // Circle
        // Delta time calculation for continous movement even in case of lag
        val circleMoveNow = System.currentTimeMillis()
        ballCircle.move((circleMoveNow - circleMoveTime) / 1000f)
        circleMoveTime = circleMoveNow
        // Check Side Collisions
        if (!ballCircle.sideCollisions()) {
            // Ball escaped
            restart()
            return

        }
        ballCircle.draw()
        // Check Square Collisions
        val collidingSquares: MutableList<GLSquareSprite> = mutableListOf()
        var didCollide = 0
        for (square in brickSquares.iterator()) {
            val doesCollide = square.circleCollision(ballCircle)
            square.draw()
            if (doesCollide != 0) {
                collidingSquares.add(square)
                didCollide = doesCollide
            }
        }
        if (didCollide == 1 || didCollide == 3) {
            ballCircle.velocity[0] = -ballCircle.velocity[0]
        }
        // Y Axis Collision
        if (didCollide == 2 || didCollide == 3) {
            ballCircle.velocity[1] = -ballCircle.velocity[1]
        }
        // Speeding up and shrinking paddle down
        for (i in collidingSquares) {
            bricksBroken += 1
            updateScore(bricksBroken)
            if (!brokenRed && i.color.contentEquals(floatArrayOf(1f, 0f, 0f, 1f))) {
                pegSquare.setSize((pegSquare.rectF.right - pegSquare.rectF.left).absoluteValue / 2, (pegSquare.rectF.top - pegSquare.rectF.bottom).absoluteValue)
                increaseBallSpeed()
                brokenRed = true
            }
            if (!brokenOrange && i.color.contentEquals(floatArrayOf(1f, 165 / 255f, 0f, 1f))) {
                increaseBallSpeed()
                brokenOrange = true
            }
            if (bricksBroken == 4 || bricksBroken == 12) {
                increaseBallSpeed()
            }
            destroyedBrickSquares.add(i)
            brickSquares.remove(i)
            // Full Clear! Very impressive!
            if (brickSquares.isEmpty()) {
                updateTime(System.currentTimeMillis() - gameStartTime)
            }
        }

        pegSquare.draw()
        // Top Collision
        val topPegColPoint = ballCircle.lineIntersectPoint(pegSquare.rectF.left, pegSquare.rectF.right, pegSquare.rectF.top, pegSquare.rectF.top)
        if (topPegColPoint != null && ballCircle.velocity[1] < 0) {
            val dx = pegSquare.rectF.centerX() - ballCircle.centerX
            val dy = pegSquare.rectF.centerY() - ballCircle.centerY
            ballCircle.setVelocity(-dx, -dy)
        }
    }

    fun movePeg(dx: Float) {
    var boundedDx = dx
        if (pegSquare.rectF.left + dx <= -1  && dx < 0 ) {
            boundedDx = dx.coerceAtLeast(-1 - pegSquare.rectF.left)
        }
        else if (pegSquare.rectF.right + dx >= 1 && dx > 0) {
            boundedDx = dx.coerceAtMost(1 - pegSquare.rectF.right)
        }
        pegSquare.rectF.offset(boundedDx, 0f)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GLCircleSprite.screenHeight = height.toFloat()
        GLCircleSprite.screenWidth = width.toFloat()
        circleMoveTime = System.currentTimeMillis()
    }
}