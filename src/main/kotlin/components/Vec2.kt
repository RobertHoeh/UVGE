package components

data class Vec2(var x: Float, var y: Float) {
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    operator fun plus(other: Vec2): Vec2 =
        Vec2(x + other.x, y + other.y)
}
