package components

import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color

data class TextInfo(
    var pos: Pair<Float, Float>,
    var color: Color?,
    var size: Float,
    var fontface: String?,
    var align: Int?
)

data class FontInfo(
    val alias: String,
    val path: String
)

class TextComponent(var text: String, var textInfo: TextInfo) {
    fun drawText(vg: Long) {
        nvgFontFace(vg, textInfo.fontface ?: "Roboto")
        nvgFontSize(vg, textInfo.size)
        nvgTextAlign(vg, textInfo.align ?: NVG_ALIGN_LEFT)
        stackPush().use { stack ->
            nvgFillColor(
                vg,
                (textInfo.color ?: Color.BLACK).toNVGColor(stack)
            )
        }
        nvgText(vg, textInfo.pos.first, textInfo.pos.second, text)
    }
}