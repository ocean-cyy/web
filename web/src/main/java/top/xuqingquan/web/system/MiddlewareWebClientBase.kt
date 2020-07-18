package top.xuqingquan.web.system

import android.webkit.WebViewClient

open class MiddlewareWebClientBase : WebViewClientDelegate {
    private var mMiddleWareWebClientBase: MiddlewareWebClientBase? = null

    override var delegate: WebViewClient?
        get() = super.delegate
        set(delegate) {
            super.delegate = delegate

        }

    constructor(client: MiddlewareWebClientBase) : super(client) {
        this.mMiddleWareWebClientBase = client
    }

    protected constructor(client: WebViewClient?) : super(client)

    protected constructor() : super(null)

    operator fun next(): MiddlewareWebClientBase? {
        return this.mMiddleWareWebClientBase
    }

    fun enq(middleWrareWebClientBase: MiddlewareWebClientBase): MiddlewareWebClientBase {
        super.delegate = middleWrareWebClientBase
        this.mMiddleWareWebClientBase = middleWrareWebClientBase
        return middleWrareWebClientBase
    }

}
