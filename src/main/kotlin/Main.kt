import components.FontInfo
import components.TextComponent
import components.TextInfo
import engine.Engine
import engine.components.TextRenderer
import java.awt.Color
import org.lwjgl.nanovg.NanoVG.*

fun main() {
    val engine = Engine(arrayOf(
        TextRenderer(
            arrayOf(
                FontInfo("Roboto", "src/main/resources/fonts/Roboto-Regular.ttf")
            ),
            mutableListOf(
                TextComponent(
                    "Hello World!",
                    TextInfo(
                        Pair(0f, 0f),
                        Color.BLACK,
                        20f,
                        "Roboto",
                        NVG_ALIGN_LEFT or NVG_ALIGN_TOP
                    )
                )
            )
        )
    ))
    engine.run()
}
