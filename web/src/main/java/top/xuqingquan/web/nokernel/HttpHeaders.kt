package top.xuqingquan.web.nokernel

import android.net.Uri

/**
 * Created by 许清泉 on 2019-07-08 20:53
 */
class HttpHeaders private constructor() {

    companion object {
        @JvmStatic
        fun create() = HttpHeaders()
    }

    private var mHeaders = mutableMapOf<String, MutableMap<String, String>?>()

    fun getHeaders(url: String): Map<String, String>? {
        val subUrl = subBaseUrl(url)
        if (mHeaders[subUrl] == null) {
            val headers = mutableMapOf<String, String>()
            mHeaders[subUrl] = headers
            return headers
        }
        return mHeaders[subUrl]
    }

    fun additionalHttpHeader(url_o: String?, k: String, v: String) {
        var url = url_o ?: return
        url = subBaseUrl(url)
        val mHeaders = mHeaders
        var headersMap = mHeaders[subBaseUrl(url)]
        if (null == headersMap) {
            headersMap = mutableMapOf()
        }
        headersMap[k] = v
        mHeaders[url] = headersMap
    }

    fun additionalHttpHeaders(url: String?, headers: MutableMap<String, String>) {
        if (url == null) {
            return
        }
        val subUrl = subBaseUrl(url)
        val mHeaders = mHeaders
        var headersMap: MutableMap<String, String>? = headers
        if (null == headersMap) {
            headersMap = mutableMapOf()
        }
        mHeaders[subUrl] = headersMap
    }

    fun removeHttpHeader(url: String?, k: String) {
        if (null == url) {
            return
        }
        val subUrl = subBaseUrl(url)
        val mHeaders = mHeaders
        val headersMap = mHeaders[subUrl]
        headersMap?.remove(k)
    }

    fun isEmptyHeaders(url_o: String): Boolean {
        var url = url_o
        url = subBaseUrl(url)
        val heads = getHeaders(url)
        return heads.isNullOrEmpty()
    }

    private fun subBaseUrl(originUrl: String): String {
        if (originUrl.isEmpty()) {
            return originUrl
        }
        val originUri = Uri.parse(originUrl)
        return "${originUri.scheme}://${originUri.authority}"
    }

}