package top.xuqingquan.web.publics

import android.view.View
import android.webkit.WebChromeClient

interface IVideo {

    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)

    fun onHideCustomView()

    fun isVideoState(): Boolean

}
