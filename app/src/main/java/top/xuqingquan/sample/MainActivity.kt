package top.xuqingquan.sample

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.tencent.smtt.export.external.extension.proxy.ProxyWebChromeClientExtension
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.export.external.interfaces.MediaAccessPermissionsCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebViewClient
import top.xuqingquan.sample.databinding.ActivityMainBinding
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.OpenOtherPageWays
import top.xuqingquan.web.nokernel.PermissionInterceptor
import top.xuqingquan.web.system.AbsAgentWebSettings
import top.xuqingquan.web.system.IAgentWebSettings

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private val url = "https://dev4.thread0.com/h5/watermark/#/"
//    private val url = "http://debugtbs.qq.com/"

    private lateinit var agentWeb: AgentWeb

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
//        binding.webview.settings.javaScriptEnabled = true
//        binding.webview.webViewClient = object :WebViewClient(){}
//        binding.webview.webChromeClient = object :WebChromeClient(){
//            override fun onGeolocationPermissionsHidePrompt() {
//                super.onGeolocationPermissionsHidePrompt()
//            }
//
//            override fun onGeolocationPermissionsShowPrompt(
//                origin: String?, callback: GeolocationPermissionsCallback?
//            ) {
//                super.onGeolocationPermissionsShowPrompt(origin, callback)
//                Log.e(TAG, "onGeolocationPermissionsShowPrompt: origin=${origin}" )
//            }
//        }
//        binding.webview.webChromeClientExtension = object :ProxyWebChromeClientExtension(){
//            override fun onPermissionRequest(
//                origin: String?,
//                resources: Long,
//                callback: MediaAccessPermissionsCallback?
//            ): Boolean {
//                callback?.invoke(origin,resources,true)
//                Log.e(TAG, "onPermissionRequest: origin=${origin},resources=$resources" )
//                return true
//            }
//        }
//        binding.webview.loadUrl(url)
        agentWeb = AgentWeb
            .with(this)
            .setAgentWebParent(binding.rootView, ViewGroup.LayoutParams(-1, -1))
            .useDefaultIndicator()
            .interceptUnknownUrl()
            .setOpenOtherPageWays(OpenOtherPageWays.ASK)
            .setProxyWebChromeClientExtension(object :ProxyWebChromeClientExtension(){
                override fun onPermissionRequest(
                    origin: String?,
                    resources: Long,
                    callback: MediaAccessPermissionsCallback?
                ): Boolean {
                    callback?.invoke(origin,resources,true)
                    Log.e(TAG, "onPermissionRequest: origin=${origin},resources=$resources" )
                    return true
                }
            })
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