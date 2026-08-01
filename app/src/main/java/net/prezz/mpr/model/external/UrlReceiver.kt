package net.prezz.mpr.model.external

fun interface UrlReceiver {

    fun receiveUrls(urls: Array<String>)
}
