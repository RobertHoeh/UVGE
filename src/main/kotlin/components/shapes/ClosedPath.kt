package components.shapes

import components.toNVGColor
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color

class ClosedPath (
    val curves: MutableList<LineLike>,
    val strokeColor: Color = Color.BLACK,
    val fillColor: Color = Color.WHITE,
    val strokeWidth: Float = 1f
) : Drawable {
    override fun draw(vg: Long) {
        nvgBeginPath(vg)

        nvgMoveTo(vg, curves.first().start.x, curves.first().start.y)
        for (curve in curves) {
            if (curve is Bezier) {
                val (_, a, b, end) = curve
                nvgBezierTo(vg, a.x, a.y, b.x, b.y, end.x, end.y)
            }
            if (curve is Line) {
                val (_, end) = curve
                nvgLineTo(vg, end.x, end.y)
            }
        }

        stackPush().use { stack ->
            nvgStrokeColor(vg, strokeColor.toNVGColor(stack))
            nvgFillColor(vg, fillColor.toNVGColor(stack))
        }

         nvgFill(vg)

        nvgStrokeWidth(vg, strokeWidth)
        nvgStroke(vg)
        nvgClosePath(vg)
    }
}