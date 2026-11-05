package components.shapes

interface Animatable : Drawable {
    fun draw(vg: Long, time: Double)

    override fun draw(vg: Long) {}
}