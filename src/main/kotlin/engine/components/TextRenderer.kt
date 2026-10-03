package engine.components

import components.FontInfo
import components.TextComponent
import engine.EngineTemplate
import engine.NeedsNVG
import engine.NeedsWindow
import org.lwjgl.nanovg.NanoVG.*

class TextRenderer(
    val fonts: Array<FontInfo>,
    val textComponents: MutableList<TextComponent>
) : EngineTemplate, NeedsWindow, NeedsNVG {
    constructor(fonts: Array<FontInfo>) : this(fonts, mutableListOf())

    constructor(textComponents: MutableList<TextComponent>) : this(
        arrayOf(FontInfo("Roboto", "src/main/resources/fonts/Roboto-Regular.ttf")),
        textComponents
    )

    override var vg: Long = 0
    override var window: Long? = null

    override fun init() {
        for ((alias, path) in fonts) {
            val id = nvgCreateFont(vg, alias, path)
            check(id != -1) { "Failed to load font $alias at $path." }
        }
    }

    override fun loop() {
        val (width, height, pixelRatio) = getWindowDimensions()

        nvgBeginFrame(vg, width, height, pixelRatio)
        for (component in textComponents) {
            component.drawText(vg)
        }
        nvgEndFrame(vg)
    }
}