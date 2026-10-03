package components

import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color

data class Bezier(val start: Vec2,
                  val a: Vec2,
                  val b: Vec2,
                  val end: Vec2,
                  val color: Color = Color.BLACK,
                  val width: Float = 1f
) {
    fun draw(vg: Long) {
        nvgBeginPath(vg)

        nvgMoveTo(vg, start.x, start.y)
        nvgBezierTo(vg, a.x, a.y, b.x, b.y, end.x, end.y)
        stackPush().use { stack ->
            nvgStrokeColor(vg, color.toNVGColor(stack))
        }
        nvgStrokeWidth(vg, width)
        nvgStroke(vg)
        nvgClosePath(vg)
    }
}