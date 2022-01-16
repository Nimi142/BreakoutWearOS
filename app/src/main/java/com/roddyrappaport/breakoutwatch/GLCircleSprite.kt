package com.roddyrappaport.breakoutwatch

import android.opengl.GLES20
import android.util.Log
import org.intellij.lang.annotations.Language
import java.lang.Math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.absoluteValue
import kotlin.math.sign


class GLCircleSprite(private val color: FloatArray) {
    companion object {
        var screenHeight = 280f
        var screenWidth = 280f
    }
    var centerX = 0f
    var centerY = 0f
    var radius = 0f
    var velocity = floatArrayOf(0f, 0f)
    var speed = 0.015f

    private fun createVertexBuffer(): FloatBuffer {
        val newSquareCoords = floatArrayOf(
            centerX - radius, centerY + radius , 0f,  // Top left
            centerX - radius, centerY - radius, 0f,  // Bottom Left
            centerX + radius, centerY - radius, 0f,  // Bottom Right
            centerX + radius, centerY + radius, 0f,  // Top Right
        )
        // (# of coordinate values * 4 bytes per float)
        return ByteBuffer.allocateDirect(newSquareCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(newSquareCoords)
                position(0)
            }
        }
    }

    @Language("GLSL")
    private val vertexShaderCode =
        """
            attribute vec4 vPosition;
            void main() {
                gl_Position = vPosition;
            }
        """.trimIndent()

    @Language("GLSL")
    private val fragmentShaderCode =
        """
            precision lowp float;
            uniform vec2 circleCoord;
            uniform vec4 vColor;
            uniform float aRadius;
            uniform float sWidth;
            uniform float sHeight;
            void main() {
                vec2 alignedPointCoord = vec2(gl_FragCoord.x / (sHeight / 2.) - 1., gl_FragCoord.y / (sWidth / 2.) - 1.);
                float dist = distance(alignedPointCoord, circleCoord);
                if (dist <= aRadius) {
                    gl_FragColor = vColor;
                }
                else {
                    gl_FragColor = vec4(0., 0., 0., 1.);
                }
            }
        """.trimIndent()

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    // initialize vertex byte buffer for shape coordinates

    // initialize byte buffer for the draw list
    private val drawListBuffer: ShortBuffer =
        // (# of coordinate values * 2 bytes per short)
        ByteBuffer.allocateDirect(drawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }
        }
    private var mProgram: Int

    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private val vertexStride: Int = GLSquareSprite.COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    init {
        val vertexShader: Int = GLSquareSprite.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int =
            GLSquareSprite.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }

    fun draw() {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {
            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)
            GLES20.glVertexAttrib4f(it, 50f, 50f, 0f, 1f)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                GLSquareSprite.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                createVertexBuffer()
            )

            // get handle to fragment shader's vColor member
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "sHeight"), screenHeight)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "sWidth"), screenWidth)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mProgram, "aRadius"), radius)
            GLES20.glUniform2f(GLES20.glGetUniformLocation(mProgram, "circleCoord"), centerX, centerY)
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // Draw the triangle
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.count(), GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }
    }

    fun lineIntersectPoint(x1: Float, x2: Float, y1: Float, y2: Float): Pair<Float, Float>? {
        // Shifting origin point to circle center
        var p1 = Pair(x1 - centerX, y1 - centerY)
        var p2 = Pair(x2 - centerX, y2 - centerY)
        // Transforming space to axis align (y) the line
        var shiftAngle = atan2((p2.second - p1.second).toDouble(), (p2.second - p1.second).toDouble())
        p1 = Pair(
            cos(shiftAngle).toFloat() * p1.first - sin(shiftAngle).toFloat() * p1.second,
            cos(shiftAngle).toFloat() * p1.second + sin(shiftAngle).toFloat() * p1.first
        )
        p2 = Pair(
            cos(shiftAngle).toFloat() * p2.first - sin(shiftAngle).toFloat() * p2.second,
            cos(shiftAngle).toFloat() * p2.second + sin(shiftAngle).toFloat() * p2.first
        )
        // Checking if new line intersects circle
        if (p1.second.absoluteValue > radius) return null
        var collisionPoint = if (p1.first.sign == p2.first.sign) {
            val minPoint = if (p1.first.absoluteValue > p2.first.absoluteValue) p2 else p1
            if ((minPoint.second * minPoint.second + minPoint.first * minPoint.first) > radius * radius) {
                return null
            }
            minPoint

        }
        else Pair(0f, p1.second)
        // Shifting back
        shiftAngle = -shiftAngle
        collisionPoint = Pair(
            cos(shiftAngle).toFloat() * collisionPoint.first - sin(shiftAngle).toFloat() * collisionPoint.second,
            cos(shiftAngle).toFloat() * collisionPoint.second + sin(shiftAngle).toFloat() * collisionPoint.first
        )
        // Shifting origin back
        return Pair(collisionPoint.first + centerX, collisionPoint.second + centerY)
    }

    fun lineIntersect(x1: Float, x2: Float, y1: Float, y2: Float): Boolean {
        return lineIntersectPoint(x1, x2, y1, y2) != null
    }

    // Returns true if survives
    fun sideCollisions(): Boolean {
        if (centerX + radius >= 1f || centerX - radius <= -1){
            centerX.coerceIn(-1 + radius, 1 - radius)
            velocity[0] = velocity[0].absoluteValue * -centerX.sign
        }
        if (centerY + radius >= 1f){
            centerY = 1 - radius
            reflectYVel()
        }
        else if (centerY + 2 * radius <= -1f) {
            return false
        }
        return true
    }

    // In seconds
    fun move(deltaTime: Float) {
        centerX += velocity[0] * deltaTime * 60
        centerY += velocity[1] * deltaTime * 60
    }

    // n for normalized, y completes to one
    fun setVelocity(xComp: Float, yComp: Float) {
        val velAngle = atan2(yComp.toDouble(), xComp.toDouble())
        velocity[0] = speed * cos(velAngle).toFloat()
        velocity[1] = speed * sin(velAngle).toFloat()

    }

    fun setVelocity(newVx: Float) {
        setVelocity(newVx, 0f)
    }

    fun reflectXVel() {
        velocity[0] = -velocity[0]
    }

    fun reflectYVel() {
        velocity[1] = -velocity[1]
    }
}