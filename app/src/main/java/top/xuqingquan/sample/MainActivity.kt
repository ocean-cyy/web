package top.xuqingquan.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.OpenOtherPageWays
import top.xuqingquan.web.nokernel.PermissionInterceptor

class MainActivity : AppCompatActivity() {

    private val url = "https://m.baidu.com/s?word=拍照识图"

    private lateinit var agentWeb: AgentWeb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        agentWeb = AgentWeb
            .with(this)
            .setAgentWebParent(rootView, ViewGroup.LayoutParams(-1, -1))
            .useDefaultIndicator()
            .setPermissionInterceptor(object : PermissionInterceptor {
                override fun intercept(
                    url: String?,
                    permissions: Array<String>,
                    action: String
                ): Boolean {
                    Timber.d("url===>$url")
                    Timber.d("permissions===>$permissions")
                    Timber.d("action===>$action")
                    return false
                }
            })
            .setOpenOtherPageWays(OpenOtherPageWays.DISALLOW)
            .createAgentWeb()
            .ready()
            .get()
        agentWeb.urlLoader?.loadUrl(url)
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