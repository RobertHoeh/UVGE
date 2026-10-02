package components

import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.system.MemoryStack
import java.awt.Color

data class TextInfo(
    var pos: Pair<Float, Float>,
    var color: Color?,
    var size: Float,
    var fontface: String?,
    var align: Int?
) {
    constructor(map: Map<String, Any>) : this(
        pos = (map["pos"] as Pair<*, *>).let {
            require(it.first is Float && it.second is Float) {
                "pos must be a Pair<Float,Float>"
            }
            @Suppress("UNCHECKED_CAST")
            it as Pair<Float, Float>
        },
        color = map["color"] as? Color,
        size = map["size"] as Float,
        fontface = map["fontface"] as? String,
        align = map["align"] as? Int
    )
}

data class FontInfo(
    val alias: String,
    val path: String
)

fun Color.toNVGColor(stack: MemoryStack): NVGColor {
    val color = NVGColor.malloc(stack)
    nvgRGBA(
        this.red.toByte(),
        this.green.toByte(),
        this.blue.toByte(),
        this.alpha.toByte(),
        color
    )
    return color
}

class TextComponent(var text: String, var textInfo: TextInfo) {
    constructor(text: String, textMap: Map<String, Any>) :
            this(text, TextInfo(textMap))

    fun drawText(vg: Long) {
        nvgFontFace(vg, textInfo.fontface ?: "Roboto")
        nvgFontSize(vg, textInfo.size)
        nvgTextAlign(vg, textInfo.align ?: NVG_ALIGN_LEFT)
        MemoryStack.stackPush().use { stack ->
            nvgFillColor(
                vg,
                (textInfo.color ?: Color.BLACK).toNVGColor(stack)
            )
        }
        nvgText(vg, textInfo.pos.first, textInfo.pos.second, text)
    }
}