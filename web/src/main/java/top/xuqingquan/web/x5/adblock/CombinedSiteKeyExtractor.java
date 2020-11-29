package top.xuqingquan.web.x5.adblock;

import android.annotation.SuppressLint;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import org.adblockplus.libadblockplus.sitekey.SiteKeysConfiguration;

import timber.log.Timber;
import top.xuqingquan.web.x5.AdblockWebView;

/**
 * Decides what extractor has to be used by analyzing the data
 * that {@link AdblockWebView} has.
 * In particular, it reads the {@link WebResourceRequest#isForMainFrame()} property
 * in order to understand, if we need to fire fallback extractor
 */
// Mainly exists for composing site key extraction and decoupling it from the WebView.
// Using both extractors directly in AdblockWebView might be error-prone
// because one might forgot that some aspects of it
// for example, that `JsSiteKeyExtractor` ads a javascript interface handler
// to the WebView
@SuppressLint("NewApi")
public class CombinedSiteKeyExtractor implements SiteKeyExtractor {
    private final SiteKeyExtractor httpExtractor;
    private final SiteKeyExtractor jsExtractor;

    @SuppressWarnings("WeakerAccess")
    public CombinedSiteKeyExtractor(final AdblockWebView webView) {
        httpExtractor = new HttpHeaderSiteKeyExtractor(webView);
        // by calling it new javascript interface handler added to the WebView
        jsExtractor = new JsSiteKeyExtractor(webView);
    }

    /*
    This implementation has the right to judge when to call what extractor
    On the main frame it calls JsSiteKeyExtractor, otherwise falls back to HttpHeaderSiteKeyExtractor

    This is to remove the obligation for the concrete implementations to decide
    Any of it may be used directly and will be doing its job properly for any request
     */
    @Override
    public WebResourceResponse extract(final WebResourceRequest frameRequest) {
        // at this point non-frame requests must have been filtered by ContentTypeDetector
        // so this presumably all non-main frame requests are of SUBDOCUMENT type (frames and iframes)
        if (!frameRequest.isForMainFrame()) {
            Timber.d("Falling back to native sitekey requests for %s",
                    frameRequest.getUrl().toString());
            return httpExtractor.extract(frameRequest);
        }

        return AdblockWebView.WebResponseResult.ALLOW_LOAD;
    }

    @Override
    public void setSiteKeysConfiguration(final SiteKeysConfiguration siteKeysConfiguration) {
        httpExtractor.setSiteKeysConfiguration(siteKeysConfiguration);
        jsExtractor.setSiteKeysConfiguration(siteKeysConfiguration);
    }

    @Override
    public void startNewPage() {
        httpExtractor.startNewPage();
        jsExtractor.startNewPage();
    }

    @Override
    public boolean waitForSitekeyCheck(final WebResourceRequest request) {
        final boolean httpWaited = httpExtractor.waitForSitekeyCheck(request);
        final boolean jsWaited = jsExtractor.waitForSitekeyCheck(request);
        return httpWaited || jsWaited;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        httpExtractor.setEnabled(enabled);
        jsExtractor.setEnabled(enabled);
    }
}
