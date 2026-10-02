package engine.components

import engine.EngineTemplate
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.nanovg.NanoVGGL3.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL

open class Core : EngineTemplate {
    companion object {
        var WINDOW_SIZE = Pair(800, 600)
    }

    var window: Long? = null
    var vg: Long = 0

    fun createWindow() {
        glfwDefaultWindowHints()

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        glfwWindowHint(GLFW_STENCIL_BITS, 8)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        window = glfwCreateWindow(WINDOW_SIZE.first, WINDOW_SIZE.second, "Hello World!", NULL, NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window!")
    }

    fun createNVG() {
        vg = nvgCreate(NVG_ANTIALIAS or NVG_STENCIL_STROKES)
        check(vg != 0L) { "Failed to create NanoVG context." }
    }

    fun setWindowPos() {
        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1)
            val pHeight = stack.mallocInt(1)

            glfwGetWindowSize(window!!, pWidth, pHeight)

            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            glfwSetWindowPos(
                window!!,
                (vidmode!!.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            )
        }
    }

    override fun init() {
        println("Hello LWJGL ${Version.getVersion()}!")
        GLFWErrorCallback.createPrint(System.err).set()

        if (!glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        createWindow()
        setWindowPos()

        glfwSetKeyCallback(window!!, object: GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true)
            }
        })

        glfwMakeContextCurrent(window!!)
        GL.createCapabilities()

        createNVG()

        glfwSwapInterval(1)
        glfwShowWindow(window!!)
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f)
    }

    fun beginFrame() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
    }

    fun endFrame() {
        glfwSwapBuffers(window!!)
        glfwPollEvents()
    }

    override fun loop() {}

    override fun cleanup() {
        nvgDelete(vg)
        glfwFreeCallbacks(window!!)
        glfwDestroyWindow(window!!)

        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }
}