package top.xuqingquan.web.system.adblock.content_type;

import android.webkit.WebResourceRequest;

import org.adblockplus.libadblockplus.FilterEngine;

/**
 * Created by 许清泉 on 11/28/20 1:34 AM
 */
public interface ContentTypeDetector {
    /**
     * Detects ContentType for given URL and headers
     *
     * @param request WebResourceRequest that contains all required
     *                info HTTP headers of incoming request including url and headers
     * @return ContentType or `null` if not detected
     */
    FilterEngine.ContentType detect(final WebResourceRequest request);
}