package engine

import engine.components.Core
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

class Engine(val renderers: Array<EngineTemplate>) : EngineTemplate {
    val core = Core()

    var window: Long? = 0
    var vg: Long = 0

    override fun init() {
        core.init()

        this.window = core.window
        this.vg = core.vg

        for (renderer in renderers) {
            if (renderer is NeedsWindow)
                renderer.window = window
            if (renderer is NeedsNVG)
                renderer.vg = vg

            renderer.init()
        }
    }

    override fun loop() {
        while (!glfwWindowShouldClose(window!!)) {
            core.beginFrame()
            for (renderer in renderers)
                renderer.loop()
            core.endFrame()
        }
    }

    override fun cleanup() {
        for (renderer in renderers)
            renderer.cleanup()
        core.cleanup()
    }

    fun run() {
        init()
        loop()
        cleanup()
    }
}