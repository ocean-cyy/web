package top.xuqingquan.web.sonic

import android.content.Context
import com.tencent.sonic.sdk.SonicConfig
import com.tencent.sonic.sdk.SonicEngine
import com.tencent.sonic.sdk.SonicSession
import com.tencent.sonic.sdk.SonicSessionConfig
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.system.MiddlewareWebClientBase
import top.xuqingquan.web.x5.MiddlewareWebClientBase as X5MiddlewareWebClientBase

/**
 * Create by 许清泉 on 2020/8/22 21:27
 */
class SonicImpl(private val url: String, private val context: Context) {

    private var sonicSession: SonicSession? = null
    private var sonicSessionClient: SonicSessionClientImpl? = null

    fun onCreateSession() {
        //init sonic engine if necessary, or maybe u can do this when application created
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(
                SonicRuntimeImpl(context.applicationContext),
                SonicConfig.Builder().build()
            )
        }
        val sessionConfigBuilder = SonicSessionConfig.Builder()
        sessionConfigBuilder.setSupportLocalServer(true)
        sonicSession = SonicEngine.getInstance().createSession(url, sessionConfigBuilder.build())
        sonicSession?.let {
            sonicSessionClient = SonicSessionClientImpl()
            it.bindClient(sonicSessionClient)
        }
    }

    fun createSonicClientMiddleWare(): MiddlewareWebClientBase {
        return SonicWebViewClient(sonicSession)
    }

    fun createX5SonicClientMiddleWare(): X5MiddlewareWebClientBase {
        return SonicX5WebViewClient(sonicSession)
    }

    fun bindAgentWeb(agentWeb: AgentWeb) {
        if (sonicSessionClient != null) {
            sonicSessionClient?.bindAgentWeb(agentWeb)
            sonicSessionClient?.clientReady()
        } else {
            agentWeb.urlLoader?.loadUrl(url)
        }
    }

    fun destroy() {
        sonicSession?.destroy()
        sonicSession = null
    }

}
