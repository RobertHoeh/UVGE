package engine.components

import components.shapes.Animatable
import components.shapes.Drawable
import engine.EngineTemplate
import engine.NeedsNVG
import engine.NeedsWindow
import org.lwjgl.glfw.GLFW.*

class ShapeRenderer(var curves: MutableList<Drawable>) : EngineTemplate, NeedsWindow, NeedsNVG {
    constructor() : this(mutableListOf())

    override var window: Long? = null
    override var vg: Long = 0

    override fun init() {}

    override fun loop() {
        for (curve in curves) {
            if (curve is Animatable)
                curve.draw(vg, glfwGetTime())
            curve.draw(vg)
        }
    }
}
