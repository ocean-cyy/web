package top.xuqingquan.web.system.adblock;

import org.adblockplus.libadblockplus.sitekey.SiteKeysConfiguration;

import java.lang.ref.WeakReference;

import top.xuqingquan.web.system.AdblockWebView;

/*
 * This is base implementation of SiteKeyExtractor
 */
@SuppressWarnings("WeakerAccess") // API
public abstract class BaseSiteKeyExtractor implements SiteKeyExtractor {
    public static final int RESOURCE_HOLD_MAX_TIME_MS = 1000;

    private SiteKeysConfiguration siteKeysConfiguration;
    private boolean isEnabled = true;
    protected final WeakReference<AdblockWebView> webViewWeakReference;

    protected BaseSiteKeyExtractor(final AdblockWebView webView) {
        webViewWeakReference = new WeakReference<>(webView);
    }

    /**
     * Returns the site key config that can be used to retrieve
     * {@link org.adblockplus.libadblockplus.sitekey.SiteKeyVerifier} and verify the site key
     *
     * @return an instance of SiteKeysConfiguration
     */
    protected SiteKeysConfiguration getSiteKeysConfiguration() {
        return siteKeysConfiguration;
    }

    @Override
    public void setSiteKeysConfiguration(final SiteKeysConfiguration siteKeysConfiguration) {
        this.siteKeysConfiguration = siteKeysConfiguration;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        isEnabled = enabled;
    }
}
