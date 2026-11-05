package components

import components.shapes.Drawable
import java.awt.Color
import java.util.Stack
import kotlin.math.*
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryStack


// I'll be honest, this file is mostly vibe-coded.

/**
 * [width] x [height] is the intended size in pixels the image is drawn at. If left null, that
 * dimension defaults to the current GL viewport size. The path is fitted (aspect ratio kept) into
 * that size and centered in the viewport, so the path's own units don't matter.
 * Set [flipY] for y-up sources such as potrace output.
 */
open class SVGImage(
    pathData: String,
    val fill: Color = Color.BLACK,
    val width: Float? = null,
    val height: Float? = null,
    val flipY: Boolean = false
) : Drawable {
    // Either a command letter or a number (handles ".5.5", "-3-4" and "1e-5")
    val commands = """[MmZzLlHhVvCcSsQqTtAa]|[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?""".toRegex()
        .findAll(pathData)
        .map { it.value }
        .toList()

    val commandStack = Stack<String>()

    var lastCommand = ""
    var lastPoint = Vec2(0f, 0f)
    var startPoint = Vec2(0f, 0f)
    var lastControl = Vec2(0f, 0f)

    // nvg works in logical pixels; set this to your nvgBeginFrame pixel ratio on HiDPI screens
    var pixelRatio = 1f

    // Bounds of the raw path, found once with a dry run (measuring = true) that doesn't touch nvg
    private var measuring = false
    private var bounds: FloatArray? = null          // minX, minY, maxX, maxY
    private var minX = 0f; private var minY = 0f
    private var maxX = 0f; private var maxY = 0f
    private var curX = 0f; private var curY = 0f

    override fun draw(vg: Long) {
        val b = bounds ?: measure(vg)
        val boxW = b[2] - b[0]
        val boxH = b[3] - b[1]
        if (boxW <= 0f || boxH <= 0f) return

        val viewport = IntArray(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        val viewW = viewport[2] / pixelRatio
        val viewH = viewport[3] / pixelRatio

        // Fit the path's box into the intended size, then center that in the viewport
        val scale = min((width ?: viewW) / boxW, (height ?: viewH) / boxH)

        nvgSave(vg)
        nvgTranslate(vg, (viewW - boxW * scale) / 2f, (viewH - boxH * scale) / 2f)
        nvgScale(vg, scale, scale)
        if (flipY) {
            nvgScale(vg, 1f, -1f)
            nvgTranslate(vg, -b[0], -b[3])
        } else {
            nvgTranslate(vg, -b[0], -b[1])
        }
        run(vg)
        nvgRestore(vg)
    }

    private fun measure(vg: Long): FloatArray {
        measuring = true
        minX = Float.MAX_VALUE; minY = Float.MAX_VALUE
        maxX = -Float.MAX_VALUE; maxY = -Float.MAX_VALUE
        run(vg)
        measuring = false
        return floatArrayOf(minX, minY, maxX, maxY).also { bounds = it }
    }

    private fun run(vg: Long) {
        var insideShape = false

        // Rebuild the stack every run so the image can be drawn more than once
        commandStack.clear()
        for (command in commands.reversed()) {
            commandStack.push(command)
        }
        lastCommand = ""
        lastPoint = Vec2(0f, 0f)
        startPoint = Vec2(0f, 0f)
        lastControl = Vec2(0f, 0f)
        curX = 0f; curY = 0f

        while (commandStack.isNotEmpty()) {
            if (!insideShape) {
                insideShape = true
                if (!measuring) nvgBeginPath(vg)
            }
            // A number where a command should be means the previous command repeats
            val command = if (commandStack.peek()[0].isLetter()) commandStack.pop() else lastCommand
            resolveSVGStack(vg, commandStack, command)
        }

        if (measuring) return

        MemoryStack.stackPush().use { stack ->
            val color = NVGColor.malloc(stack)
            nvgRGBA(
                fill.red.toByte(), fill.green.toByte(), fill.blue.toByte(), fill.alpha.toByte(),
                color
            )
            nvgFillColor(vg, color)
            nvgFill(vg)
        }
    }

    // ------------------------------------------------------------- nvg wrappers
    // While measuring these only record bounds; otherwise they forward to nvg.

    private fun track(x: Float, y: Float) {
        minX = min(minX, x); minY = min(minY, y)
        maxX = max(maxX, x); maxY = max(maxY, y)
    }

    private fun moveTo(vg: Long, x: Float, y: Float) {
        if (measuring) track(x, y) else nvgMoveTo(vg, x, y)
        curX = x; curY = y
    }

    private fun lineTo(vg: Long, x: Float, y: Float) {
        if (measuring) track(x, y) else nvgLineTo(vg, x, y)
        curX = x; curY = y
    }

    private fun bezierTo(vg: Long, c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) {
        if (measuring) {
            // Sample the curve so control points outside the curve don't inflate the bounds
            for (i in 1..16) {
                val t = i / 16f
                val u = 1 - t
                track(
                    u * u * u * curX + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * x,
                    u * u * u * curY + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * y
                )
            }
        } else {
            nvgBezierTo(vg, c1x, c1y, c2x, c2y, x, y)
        }
        curX = x; curY = y
    }

    private fun quadTo(vg: Long, cx: Float, cy: Float, x: Float, y: Float) {
        if (measuring) {
            for (i in 1..16) {
                val t = i / 16f
                val u = 1 - t
                track(u * u * curX + 2 * u * t * cx + t * t * x, u * u * curY + 2 * u * t * cy + t * t * y)
            }
        } else {
            nvgQuadTo(vg, cx, cy, x, y)
        }
        curX = x; curY = y
    }

    fun resolveSVGStack(vg: Long, commandStack: Stack<String>, command: String) {
        when (command) {
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
                lastPoint = lSVG(vg, lastPoint)
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
            "S" -> {
                lastPoint = SSVG(vg, lastPoint)
                lastCommand = "S"
            }
            "s" -> {
                lastPoint = sSVG(vg, lastPoint)
                lastCommand = "s"
            }
            "Q" -> {
                lastPoint = QSVG(vg)
                lastCommand = "Q"
            }
            "q" -> {
                lastPoint = qSVG(vg, lastPoint)
                lastCommand = "q"
            }
            "T" -> {
                lastPoint = TSVG(vg, lastPoint)
                lastCommand = "T"
            }
            "t" -> {
                lastPoint = tSVG(vg, lastPoint)
                lastCommand = "t"
            }
            "A" -> {
                lastPoint = ASVG(vg, lastPoint)
                lastCommand = "A"
            }
            "a" -> {
                lastPoint = aSVG(vg, lastPoint)
                lastCommand = "a"
            }
            "Z", "z" -> {
                lastPoint = ZSVG(vg)
                lastCommand = "Z"
            }
            else -> throw IllegalArgumentException("Unknown or misplaced SVG path token '$command'")
        }
    }

    // ---------------------------------------------------------------- helpers

    fun num(): Float = commandStack.pop().toFloat()

    // Arc flags are single characters and may be glued to the next number ("00.5.5")
    fun popFlag(): Boolean {
        val token = commandStack.pop()
        if (token.length > 1) commandStack.push(token.substring(1))
        return token[0] == '1'
    }

    // ------------------------------------------------------------- moveto / lineto

    fun MSVG(vg: Long): Vec2 {
        val lastPoint = Vec2(num(), num())
        moveTo(vg, lastPoint.x, lastPoint.y)
        startPoint = lastPoint
        return lastPoint
    }

    fun mSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val delta = Vec2(num(), num())
        val newPoint = lastPoint + delta
        moveTo(vg, newPoint.x, newPoint.y)
        startPoint = newPoint
        return newPoint
    }

    fun LSVG(vg: Long): Vec2 {
        val lastPoint = Vec2(num(), num())
        lineTo(vg, lastPoint.x, lastPoint.y)
        return lastPoint
    }

    fun lSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val delta = Vec2(num(), num())
        val newPoint = lastPoint + delta
        lineTo(vg, newPoint.x, newPoint.y)
        return newPoint
    }

    fun HSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val newPoint = Vec2(num(), lastPoint.y)
        lineTo(vg, newPoint.x, newPoint.y)
        return newPoint
    }

    fun hSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val newPoint = Vec2(lastPoint.x + num(), lastPoint.y)
        lineTo(vg, newPoint.x, newPoint.y)
        return newPoint
    }

    fun VSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val newPoint = Vec2(lastPoint.x, num())
        lineTo(vg, newPoint.x, newPoint.y)
        return newPoint
    }

    fun vSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val newPoint = Vec2(lastPoint.x, lastPoint.y + num())
        lineTo(vg, newPoint.x, newPoint.y)
        return newPoint
    }

    // ------------------------------------------------------------- cubic curves

    fun CSVG(vg: Long): Vec2 {
        val a = Vec2(num(), num())
        val b = Vec2(num(), num())
        val lastPoint = Vec2(num(), num())
        bezierTo(vg, a.x, a.y, b.x, b.y, lastPoint.x, lastPoint.y)
        lastControl = b
        return lastPoint
    }

    fun cSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val a = lastPoint + Vec2(num(), num())
        val b = lastPoint + Vec2(num(), num())
        val newPoint = lastPoint + Vec2(num(), num())
        bezierTo(vg, a.x, a.y, b.x, b.y, newPoint.x, newPoint.y)
        lastControl = b
        return newPoint
    }

    // The first control point is the reflection of the previous cubic's second one
    // (or the current point if the previous command wasn't a cubic)
    fun reflectedCubicControl(lastPoint: Vec2): Vec2 =
        if (lastCommand in listOf("C", "c", "S", "s"))
            Vec2(2 * lastPoint.x - lastControl.x, 2 * lastPoint.y - lastControl.y)
        else lastPoint

    fun SSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val a = reflectedCubicControl(lastPoint)
        val b = Vec2(num(), num())
        val newPoint = Vec2(num(), num())
        bezierTo(vg, a.x, a.y, b.x, b.y, newPoint.x, newPoint.y)
        lastControl = b
        return newPoint
    }

    fun sSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val a = reflectedCubicControl(lastPoint)
        val b = lastPoint + Vec2(num(), num())
        val newPoint = lastPoint + Vec2(num(), num())
        bezierTo(vg, a.x, a.y, b.x, b.y, newPoint.x, newPoint.y)
        lastControl = b
        return newPoint
    }

    // ------------------------------------------------------------- quadratic curves

    fun QSVG(vg: Long): Vec2 {
        val a = Vec2(num(), num())
        val lastPoint = Vec2(num(), num())
        quadTo(vg, a.x, a.y, lastPoint.x, lastPoint.y)
        lastControl = a
        return lastPoint
    }

    fun qSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val a = lastPoint + Vec2(num(), num())
        val newPoint = lastPoint + Vec2(num(), num())
        quadTo(vg, a.x, a.y, newPoint.x, newPoint.y)
        lastControl = a
        return newPoint
    }

    // The control point is the reflection of the previous quadratic's control point
    fun reflectedQuadControl(lastPoint: Vec2): Vec2 =
        if (lastCommand in listOf("Q", "q", "T", "t"))
            Vec2(2 * lastPoint.x - lastControl.x, 2 * lastPoint.y - lastControl.y)
        else lastPoint

    fun TSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val a = reflectedQuadControl(lastPoint)
        val newPoint = Vec2(num(), num())
        quadTo(vg, a.x, a.y, newPoint.x, newPoint.y)
        lastControl = a
        return newPoint
    }

    fun tSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val a = reflectedQuadControl(lastPoint)
        val newPoint = lastPoint + Vec2(num(), num())
        quadTo(vg, a.x, a.y, newPoint.x, newPoint.y)
        lastControl = a
        return newPoint
    }

    // ------------------------------------------------------------- arcs

    fun ASVG(vg: Long, lastPoint: Vec2): Vec2 {
        val rx = num()
        val ry = num()
        val rotation = num()
        val largeArc = popFlag()
        val sweep = popFlag()
        val newPoint = Vec2(num(), num())
        arcTo(vg, lastPoint, rx, ry, rotation, largeArc, sweep, newPoint)
        return newPoint
    }

    fun aSVG(vg: Long, lastPoint: Vec2): Vec2 {
        val rx = num()
        val ry = num()
        val rotation = num()
        val largeArc = popFlag()
        val sweep = popFlag()
        val newPoint = lastPoint + Vec2(num(), num())
        arcTo(vg, lastPoint, rx, ry, rotation, largeArc, sweep, newPoint)
        return newPoint
    }

    // SVG elliptical arc: endpoint -> center parameterization (SVG 1.1 spec, appendix F.6),
    // then drawn with nvgArc under a transform that turns the unit circle into the ellipse
    fun arcTo(
        vg: Long, from: Vec2,
        rxIn: Float, ryIn: Float, rotationDeg: Float,
        largeArc: Boolean, sweep: Boolean, to: Vec2
    ) {
        if (from.x == to.x && from.y == to.y) return   // spec: omit the segment
        var rx = abs(rxIn).toDouble()
        var ry = abs(ryIn).toDouble()
        if (rx == 0.0 || ry == 0.0) {                   // spec: treat as a straight line
            lineTo(vg, to.x, to.y)
            return
        }

        val phi = Math.toRadians(rotationDeg.toDouble())
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)

        val dx2 = (from.x - to.x) / 2.0
        val dy2 = (from.y - to.y) / 2.0
        val x1p = cosPhi * dx2 + sinPhi * dy2
        val y1p = -sinPhi * dx2 + cosPhi * dy2

        // Scale the radii up if they can't span the endpoints
        val lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if (lambda > 1.0) {
            val s = sqrt(lambda)
            rx *= s
            ry *= s
        }

        val rx2 = rx * rx
        val ry2 = ry * ry
        val num = rx2 * ry2 - rx2 * y1p * y1p - ry2 * x1p * x1p
        val den = rx2 * y1p * y1p + ry2 * x1p * x1p
        var coef = sqrt(max(0.0, num / den))
        if (largeArc == sweep) coef = -coef
        val cxp = coef * (rx * y1p / ry)
        val cyp = coef * -(ry * x1p / rx)

        val centerX = cosPhi * cxp - sinPhi * cyp + (from.x + to.x) / 2.0
        val centerY = sinPhi * cxp + cosPhi * cyp + (from.y + to.y) / 2.0

        val theta1 = atan2((y1p - cyp) / ry, (x1p - cxp) / rx)
        val theta2 = atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx)
        var dTheta = theta2 - theta1
        if (sweep && dTheta < 0) dTheta += 2 * PI
        else if (!sweep && dTheta > 0) dTheta -= 2 * PI

        if (measuring) {
            // Sample points along the ellipse for the bounds
            for (i in 0..24) {
                val th = theta1 + dTheta * i / 24.0
                track(
                    (centerX + rx * cos(th) * cosPhi - ry * sin(th) * sinPhi).toFloat(),
                    (centerY + rx * cos(th) * sinPhi + ry * sin(th) * cosPhi).toFloat()
                )
            }
        } else {
            // nanovg transforms each path point as it is added, so a unit-circle arc drawn inside a
            // translated/rotated/scaled space comes out as the rotated ellipse. Save/restore keeps
            // the transform from leaking, and points already in the path are unaffected.
            // In nanovg, NVG_CW means the angle increases, which matches SVG's sweep-flag = 1.
            nvgSave(vg)
            nvgTranslate(vg, centerX.toFloat(), centerY.toFloat())
            nvgRotate(vg, phi.toFloat())
            nvgScale(vg, rx.toFloat(), ry.toFloat())
            nvgArc(vg, 0f, 0f, 1f, theta1.toFloat(), (theta1 + dTheta).toFloat(), if (sweep) NVG_CW else NVG_CCW)
            nvgRestore(vg)
        }

        // Snap to the exact endpoint so float error doesn't accumulate across segments
        lineTo(vg, to.x, to.y)
    }

    // ------------------------------------------------------------- closepath

    fun ZSVG(vg: Long): Vec2 {
        nvgClosePath(vg)
        return startPoint
    }
}