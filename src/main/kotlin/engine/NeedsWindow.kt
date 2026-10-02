package engine

import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.system.MemoryStack.stackPush
import kotlin.use

interface NeedsWindow {
    var window: Long?

    data class WindowDim(val width: Float, val height: Float, val pixelRatio: Float)

    fun getWindowDimensions(): WindowDim {
        stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val fbW = stack.mallocInt(1)
            val fbH = stack.mallocInt(1)

            glfwGetWindowSize(window!!, w, h)
            glfwGetFramebufferSize(window!!, fbW, fbH)


            val pixelRatio = fbW.get(0).toFloat() / w.get(0)
            return WindowDim(
                w.get(0).toFloat(),
                h.get(0).toFloat(),
                pixelRatio
            )
        }
    }
}