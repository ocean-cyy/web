package top.xuqingquan.web.publics

import android.view.View
import android.webkit.WebChromeClient
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient

interface IVideo {

    fun onShowCustomView(view: View?, callback: IX5WebChromeClient.CustomViewCallback?)

    fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?)

    fun onHideCustomView()

    fun isVideoState(): Boolean

}
