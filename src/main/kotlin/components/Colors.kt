package components

import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG.nvgRGBA
import org.lwjgl.system.MemoryStack
import java.awt.Color

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