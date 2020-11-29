package top.xuqingquan.web.x5.adblock;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.adblockplus.libadblockplus.sitekey.SiteKeysConfiguration;

import top.xuqingquan.web.x5.AdblockWebView;

/**
 * Created by 许清泉 on 11/28/20 1:39 AM
 */
public interface SiteKeyExtractor {
    /**
     * This method is called by the {@link AdblockWebView} during
     * {@link WebViewClient#shouldInterceptRequest(WebView, WebResourceRequest)}
     * <p/>
     * This method must perform custom HTTP request or return one of states from
     * {@link AdblockWebView.WebResponseResult}
     *
     * @param request a request that might be used for understanding
     *                additional options (e.g. is the request intended for the main frame)
     * @return a response that will be passed to
     */
    WebResourceResponse extract(WebResourceRequest request);

    /**
     * Notifies about starting of a new page
     */
    void startNewPage();

    /**
     * Blocks the calling thread while checking the sitekey
     * <p>
     * Will be removed later in a favor of setting internal WebViewClient
     * for every SiteKeyExtractor
     *
     * @param request from the
     *                {@link WebViewClient#shouldInterceptRequest(WebView, WebResourceRequest)}
     * @return true if had to wait
     */
    boolean waitForSitekeyCheck(final WebResourceRequest request);

    /**
     * This method is called by the {@link AdblockWebView} during
     * {@link AdblockWebView#setSiteKeysConfiguration(SiteKeysConfiguration)}
     * <p/>
     * You can later use siteKeysConfiguration in order to verify the sitekey
     */
    void setSiteKeysConfiguration(SiteKeysConfiguration siteKeysConfiguration);

    void setEnabled(boolean enabled);
}
