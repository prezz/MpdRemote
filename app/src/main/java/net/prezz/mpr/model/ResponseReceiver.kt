package net.prezz.mpr.model

abstract class ResponseReceiver<Response> {

    open fun buildingDatabase() {
    }

    abstract fun receiveResponse(response: Response)
}
