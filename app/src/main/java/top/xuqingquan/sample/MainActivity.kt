package top.xuqingquan.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_main.*
import top.xuqingquan.utils.StatusBarUtils
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.OpenOtherPageWays
import top.xuqingquan.web.nokernel.PermissionInterceptor
import top.xuqingquan.web.system.AbsAgentWebSettings
import top.xuqingquan.web.system.IAgentWebSettings

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private val url = "https://m.baidu.com"

    private lateinit var agentWeb: AgentWeb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        agentWeb = AgentWeb
            .with(this)
            .setAgentWebParent(rootView, ViewGroup.LayoutParams(-1, -1))
            .useDefaultIndicator()
            .interceptUnknownUrl()
            .setOpenOtherPageWays(OpenOtherPageWays.ASK)
            .setAgentWebWebSettings(object : AbsAgentWebSettings(){
                override fun bindAgentWebSupport(agentWeb: AgentWeb) {
                }

                override fun toSetting(webView: WebView?): IAgentWebSettings<*> {
                    val settings= super.toSetting(webView)
                    settings.getWebSettings()?.setGeolocationEnabled(true)
                    return settings
                }
            })
            .setPermissionInterceptor(object : PermissionInterceptor {
                override fun intercept(
                    url: String?,
                    permissions: Array<String>,
                    action: String
                ): Boolean {
                    permissions.forEach {
                        Log.d(TAG, "intercept: permissions=$it")
                    }
                    Log.d(TAG, "intercept: action=$action")
                    return false
                }
            })
            .parseThunder()
            .createAgentWeb()
            .ready()
            .get()
        agentWeb.urlLoader?.loadUrl(url)
        agentWeb.webCreator?.getWebView()?.settings?.userAgentString = ""
    }

    override fun onPause() {
        agentWeb.webLifeCycle.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        agentWeb.webLifeCycle.onResume()
    }

    override fun onDestroy() {
        agentWeb.webLifeCycle.onDestroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return agentWeb.handleKeyEvent(keyCode, event)
    }
}