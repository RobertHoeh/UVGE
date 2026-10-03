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
    val engine = Engine(arrayOf(
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
                Bezier(
                    Vec2(0,0),
                    Vec2(400,300),
                    Vec2(600,300),
                    Vec2(0,0),
                    Color.BLACK,
                    6.5f
                ),
                Line(
                    Vec2(0,0),
                    Vec2(800,600),
                    Color.BLUE,
                    5f
                ),
                ClosedPath(
                    mutableListOf(
                        Line(
                            Vec2(0,0),
                            Vec2(400,300),
                        ),
                        Line(
                            Vec2(400,300),
                            Vec2(800,0),
                        ),
                        Line(
                            Vec2(800,0),
                            Vec2(0,0)
                        )
                    )
                )
            )
        )
    ))
    engine.run()
}
