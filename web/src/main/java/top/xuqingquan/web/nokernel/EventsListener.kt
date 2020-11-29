package top.xuqingquan.web.nokernel

import org.adblockplus.libadblockplus.FilterEngine

/**
 * Created by 许清泉 on 11/29/20 7:42 PM
 * Listener for ad blocking related events.
 * However, this interface may not be in use if Adblock Plus is disabled.
 */
interface EventsListener {
    /**
     * Immutable data-class containing an auxiliary information about resource event.
     */
    open class ResourceInfo internal constructor(
        val requestUrl: String,
        val parentFrameUrls: MutableList<String>? = arrayListOf()
    )

    /**
     * Immutable data-class containing an auxiliary information about blocked resource.
     */
    class BlockedResourceInfo(
        requestUrl: String,
        parentFrameUrls: MutableList<String>?,
        val contentType: FilterEngine.ContentType
    ) : ResourceInfo(requestUrl, parentFrameUrls)

    /**
     * Whitelisting reason:
     */
    enum class WhitelistReason {
        /**
         * Document is whitelisted
         */
        DOCUMENT,

        /**
         * Domain is whitelisted by user
         */
        DOMAIN,

        /**
         * Exception filter
         */
        FILTER
    }

    /**
     * Immutable data-class containing an auxiliary information about whitelisted resource.
     */
    class WhitelistedResourceInfo(
        requestUrl: String,
        parentFrameUrls: MutableList<String>?,
        val reason: WhitelistReason
    ) : ResourceInfo(requestUrl, parentFrameUrls)

    /**
     * "Navigation" event.
     *
     *
     * This method is called when the current instance of WebView begins loading of a new page.
     * It corresponds to `onPageStarted` of `WebViewClient` and is called on the UI thread.
     */
    fun onNavigation()

    /**
     * "Resource loading blocked" event.
     *
     *
     * This method can be called on a background thread.
     * It should not block the thread for too long as it slows down resource loading.
     *
     * @param info contains auxiliary information about a blocked resource.
     */
    fun onResourceLoadingBlocked(info: BlockedResourceInfo?)

    /**
     * "Resource loading whitelisted" event.
     *
     *
     * This method can be called on a background thread.
     * It should not block the thread for too long as it slows down resource loading.
     *
     * @param info contains auxiliary information about a blocked resource.
     */
    fun onResourceLoadingWhitelisted(info: WhitelistedResourceInfo?)
}