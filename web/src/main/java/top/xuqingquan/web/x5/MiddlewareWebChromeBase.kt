package top.xuqingquan.web.x5

import com.tencent.smtt.sdk.WebChromeClient

open class MiddlewareWebChromeBase(webChromeClient: WebChromeClient? = null) :
    WebChromeClientDelegate(webChromeClient) {

    private var mMiddlewareWebChromeBase: MiddlewareWebChromeBase? = null

    override var delegate: WebChromeClient?
        get() = super.delegate
        set(delegate) {
            super.delegate = delegate
        }

    fun enq(middlewareWebChromeBase: MiddlewareWebChromeBase): MiddlewareWebChromeBase? {
        super.delegate = middlewareWebChromeBase
        this.mMiddlewareWebChromeBase = middlewareWebChromeBase
        return this.mMiddlewareWebChromeBase
    }

    operator fun next(): MiddlewareWebChromeBase? {
        return this.mMiddlewareWebChromeBase
    }

}
