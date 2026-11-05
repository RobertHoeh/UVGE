package components

import components.shapes.Animatable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.awt.Color
import java.io.File
import java.io.FileInputStream

class SVGAnim(
    pathData: List<String>,
    val flipY: Boolean = false,
    val width: Float? = null,
    val height: Float? = null,
    val fill: Color = Color.BLACK,
    val framerate: Int = 60,
    val delay: Float = 0f
) : Animatable {

    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        pathData: String,
        flipY: Boolean = false,
        width: Float? = null,
        height: Float? = null,
        fill: Color = Color.BLACK,
        framerate: Int = 60,
        delay: Float = 0f
    ) : this(
        Json.decodeFromStream<List<String>>(File(pathData).inputStream()),
        flipY,
        width,
        height,
        fill,
        framerate,
        delay
    )

    val anim: List<SVGImage> = pathData.map { SVGImage(it, fill, width, height, flipY) }

    val secondsPerFrame = 1f / framerate

    override fun draw(vg: Long, time: Double) {
        if (time > delay) {
            val frame = getFrame(time)
            frame.draw(vg)
        }
    }

    fun getFrame(time: Double): SVGImage {
        val index = ((time - delay) / secondsPerFrame).toInt()
        return if (index < anim.size)
            anim[index]
        else
            anim.last()
    }
}