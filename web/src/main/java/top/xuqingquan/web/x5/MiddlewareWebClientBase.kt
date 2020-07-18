package top.xuqingquan.web.x5

import com.tencent.smtt.sdk.WebViewClient

open class MiddlewareWebClientBase : WebViewClientDelegate {
    private var mMiddleWrareWebClientBase: MiddlewareWebClientBase? = null

    override var delegate: WebViewClient?
        get() = super.delegate
        set(delegate) {
            super.delegate = delegate

        }

    constructor(client: MiddlewareWebClientBase) : super(client) {
        this.mMiddleWrareWebClientBase = client
    }

    protected constructor(client: WebViewClient?) : super(client)

    protected constructor() : super(null)

    operator fun next(): MiddlewareWebClientBase? {
        return this.mMiddleWrareWebClientBase
    }

    fun enq(middleWrareWebClientBase: MiddlewareWebClientBase): MiddlewareWebClientBase {
        super.delegate = middleWrareWebClientBase
        this.mMiddleWrareWebClientBase = middleWrareWebClientBase
        return middleWrareWebClientBase
    }

}
