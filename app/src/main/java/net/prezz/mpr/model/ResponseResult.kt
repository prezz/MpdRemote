package net.prezz.mpr.model

class ResponseResult(val isSuccess: Boolean) {

    enum class ValueType {
        VOLUME,
        PLAYER_STATE
    }

    private val responseValues: MutableMap<ValueType, Any?> = HashMap()

    fun putResponseValue(valueType: ValueType, value: Any?): ResponseResult {
        responseValues[valueType] = value
        return this
    }

    fun getResponseValue(valueType: ValueType): Any? {
        return responseValues[valueType]
    }
}
