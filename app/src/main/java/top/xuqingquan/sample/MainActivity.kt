package top.xuqingquan.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.sonic.SonicImpl

class MainActivity : AppCompatActivity() {

    private val url = "https://www.freebuf.com/articles/system/34571.html"

    private lateinit var mSonicImpl: SonicImpl
    private lateinit var agentWeb: AgentWeb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSonicImpl = SonicImpl(url, this)
        mSonicImpl.onCreateSession()
        agentWeb = AgentWeb
            .with(this)
            .setAgentWebParent(rootView, ViewGroup.LayoutParams(-1, -1))
            .useDefaultIndicator()
            .useMiddlewareWebClient(mSonicImpl.createSonicClientMiddleWare())
            .createAgentWeb()
            .ready()
            .get()
        agentWeb.urlLoader?.loadUrl(url)
        mSonicImpl.bindAgentWeb(agentWeb)
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
        mSonicImpl.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return agentWeb.handleKeyEvent(keyCode, event)
    }
}