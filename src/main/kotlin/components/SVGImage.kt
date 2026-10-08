package components

import components.shapes.Drawable
import java.awt.Color
import java.util.Stack
import org.lwjgl.nanovg.NanoVG.*


class SVGImage(pathData: String, val fill: Color = Color.BLACK) : Drawable {
    val commands = pathData
        .split("""[\s,]+|(?=[MmZzLlHhVvCcSsQqTtAa])|(?<=[MmZzLlHhVvCcSsQqTtAa])|(?<=[\d.])(?=[-+])""".toRegex())
        .filter { it.isNotEmpty() }

    val commandStack = Stack<String>().apply {
        for (command in commands.reversed()) {
            push(command)
        }
    }

    override fun draw(vg: Long) {
        var lastCommand = ""
        var insideShape = false
        var lastPoint = Vec2(0,0)

        while (commandStack.isNotEmpty()) {
            if (!insideShape) {
                insideShape = true
                nvgBeginPath(vg)
            }
            when (commandStack.pop()) {
                "M" -> {
                    lastPoint = MSVG(vg)
                    lastCommand = "L"
                }
                "m" -> {
                    lastPoint = mSVG(vg, lastPoint)
                    lastCommand = "l"
                }
                "L" -> {
                    lastPoint = LSVG(vg)
                    lastCommand = "L"
                }
                "l" -> {
                    val lastPoint = lSVG(vg, lastPoint)
                    lastCommand = "l"
                }
                "H" -> {
                    lastPoint = HSVG(vg, lastPoint)
                    lastCommand = "H"
                }
                "h" -> {
                    lastPoint = hSVG(vg, lastPoint)
                    lastCommand = "h"
                }
                "V" -> {
                    lastPoint = VSVG(vg, lastPoint)
                    lastCommand = "V"
                }
                "v" -> {
                    lastPoint = vSVG(vg, lastPoint)
                    lastCommand = "v"
                }
                "C" -> {
                    lastPoint = CSVG(vg)
                    lastCommand = "C"
                }
                "c" -> {
                    lastPoint = cSVG(vg, lastPoint)
                    lastCommand = "c"
                }

            }
        }
    }

    fun MSVG(vg: Long): Vec2 {
        val lastPoint = Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        nvgMoveTo(vg, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun mSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val delta = Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        val newPoint = lastPoint + delta
        nvgMoveTo(vg, newPoint.x, newPoint.y)
        return newPoint
    }

    fun LSVG(vg: Long): Vec2 {
        val lastPoint = Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        nvgMoveTo(vg, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun lSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val delta = Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        val newPoint = lastPoint + delta
        nvgLineTo(vg, newPoint.x, newPoint.y)
        return newPoint
    }

    fun HSVG(vg: Long, lastPoint: Vec2): Vec2 {
        lastPoint.x = commandStack.pop().toFloat()
        nvgLineTo(vg, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun hSVG(vg: Long, lastPoint: Vec2): Vec2 {
        lastPoint.x = commandStack.pop().toFloat()
        nvgLineTo(vg, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun VSVG(vg: Long, lastPoint: Vec2): Vec2 {
        lastPoint.x = commandStack.pop().toFloat()
        nvgLineTo(vg, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun vSVG(vg: Long, lastPoint: Vec2): Vec2 {
        lastPoint.y += commandStack.pop().toFloat()
        nvgLineTo(vg, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun CSVG(vg: Long): Vec2 {
        val a = Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        val b = Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        val lastPoint = Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        nvgBezierTo(vg, a.x, a.y, b.x, b.y, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun cSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val a = lastPoint + Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        val b = lastPoint + Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        val newPoint = lastPoint + Vec2(commandStack.pop().toFloat(), commandStack.pop().toFloat())
        nvgBezierTo(vg, a.x, a.y, b.x, b.y, lastPoint.x, lastPoint.y)
        return newPoint
    }
}