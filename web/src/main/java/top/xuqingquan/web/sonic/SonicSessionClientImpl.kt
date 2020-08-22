package top.xuqingquan.web.sonic

import android.os.Bundle
import com.tencent.sonic.sdk.SonicSessionClient
import top.xuqingquan.web.AgentWeb
import java.util.HashMap

/**
 * Create by 许清泉 on 2020/8/22 20:53
 */
class SonicSessionClientImpl : SonicSessionClient() {

    private lateinit var agentWeb: AgentWeb

    fun bindAgentWeb(agentWeb: AgentWeb) {
        this.agentWeb = agentWeb
    }

    override fun loadUrl(url: String?, extraData: Bundle?) {
        if (url.isNullOrEmpty()) {
            return
        }
        agentWeb.urlLoader?.loadUrl(url)
    }

    override fun loadDataWithBaseUrl(
        baseUrl: String?, data: String?, mimeType: String?, encoding: String?, historyUrl: String?
    ) {
        if (data.isNullOrEmpty() || mimeType.isNullOrEmpty() || encoding.isNullOrEmpty()) {
            return
        }
        agentWeb.urlLoader?.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    override fun loadDataWithBaseUrlAndHeader(
        baseUrl: String?, data: String?, mimeType: String?, encoding: String?,
        historyUrl: String?, headers: HashMap<String, String>?
    ) {
        loadDataWithBaseUrl(baseUrl, data, mimeType, encoding, historyUrl)
    }
}