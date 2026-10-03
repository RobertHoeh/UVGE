package engine.components

import components.Bezier
import engine.EngineTemplate
import engine.NeedsNVG
import engine.NeedsWindow

class ShapeRenderer(var curves: MutableList<Bezier>) : EngineTemplate, NeedsWindow, NeedsNVG {
    constructor() : this(mutableListOf())

    override var window: Long? = null
    override var vg: Long = 0

    override fun init() {}

    override fun loop() {
        for (curve in curves) {
            curve.draw(vg)
        }
    }
}
