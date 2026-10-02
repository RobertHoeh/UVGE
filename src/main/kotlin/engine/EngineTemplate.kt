package engine

interface EngineTemplate {
    fun init()

    fun loop()

    fun cleanup() {}
}