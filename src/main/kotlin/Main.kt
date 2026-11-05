import components.SVGAnim
import components.SVGImage
import components.shapes.Bezier
import components.TextComponent
import components.TextInfo
import components.Vec2
import components.shapes.ClosedPath
import components.shapes.Line
import engine.Engine
import engine.components.ShapeRenderer
import engine.components.TextRenderer
import java.awt.Color
import org.lwjgl.nanovg.NanoVG.*

fun main() {
    val engine = Engine(
        arrayOf(
            TextRenderer(
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
            ),
            ShapeRenderer(
                mutableListOf(
                    SVGAnim(
                        "src/main/resources/pathData/pathData.json",
                        flipY = true,
                        framerate = 15,
                        fill = Color.WHITE
                    )
                )
            )
        ),
        Color.BLACK
    )
    engine.run()
}
