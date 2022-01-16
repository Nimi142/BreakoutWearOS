package com.roddyrappaport.breakoutwatch

import android.graphics.RectF
import android.opengl.GLES20
import android.os.Debug
import android.util.Log
import org.intellij.lang.annotations.Language
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.absoluteValue

// number of coordinates per vertex in this array
open class GLSquareSprite(val color: FloatArray) {
    companion object {
        const val COORDS_PER_VERTEX = 3

        fun loadShader(type: Int, shaderCode: String): Int {

            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            return GLES20.glCreateShader(type).also { shader ->

                // add the source code to the shader and compile it
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
            }
        }
    }

    // -1 to +1
    private var velocity = floatArrayOf(0f, 0f)

    var rectF: RectF = RectF(-0.1f, 0.1f, 0.1f, -0.1f)

    // 0 if not, 1 if X, 2 if Y, 3 if Both
    fun circleCollision(circle: GLCircleSprite): Int {
        if (rectF.contains(circle.centerX, circle.centerY)) return 3
        var res = 0
        if (circle.lineIntersect(rectF.left, rectF.left, rectF.top, rectF.bottom) || circle.lineIntersect(rectF.right, rectF.right, rectF.top, rectF.bottom)) {
            res += 1
        }
        if (circle.lineIntersect(rectF.left, rectF.right, rectF.top, rectF.top) || circle.lineIntersect(rectF.left, rectF.right, rectF.bottom, rectF.bottom)) {
            res += 2
        }
        return res
    }


    private fun createVertexBuffer(): FloatBuffer {
        val newSquareCoords = floatArrayOf(
            rectF.left, rectF.top, 0f,  // Top left
            rectF.left, rectF.bottom, 0f,  // Bottom Left
            rectF.right, rectF.bottom, 0f,  // Bottom Right
            rectF.right, rectF.top, 0f,  // Top Right
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
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
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

    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    init {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

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
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                createVertexBuffer()
            )

            // get handle to fragment shader's vColor member
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

    fun setSize(xSize: Float, ySize: Float) {
        val cx = rectF.centerX()
        val cy = rectF.centerY()
        rectF = RectF(cx - xSize / 2, cy + ySize / 2, cx + xSize / 2, cy - ySize / 2)
    }

    private fun move(dx: Float, dy: Float) {
        rectF.offset(dx, dy)
    }

}
