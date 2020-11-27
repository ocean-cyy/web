package top.xuqingquan.web.system.adblock.content_type;

import android.webkit.WebResourceRequest;

import org.adblockplus.libadblockplus.FilterEngine;

/**
 * Created by 许清泉 on 11/28/20 1:37 AM
 */
public class OrderedContentTypeDetector implements ContentTypeDetector {
    private final ContentTypeDetector[] detectors;

    /**
     * Creates an instance of a `MultipleContentTypeDetector`
     * with provided detectors
     * <p>
     * At the moment only {@link HeadersContentTypeDetector}
     * and {@link UrlFileExtensionTypeDetector} exists
     *
     * @param detectors an array of instances of {@link ContentTypeDetector}
     */
    public OrderedContentTypeDetector(final ContentTypeDetector... detectors) {
        this.detectors = detectors;
    }

    @Override
    public FilterEngine.ContentType detect(final WebResourceRequest request) {
        FilterEngine.ContentType contentType;

        for (final ContentTypeDetector detector : detectors) {
            contentType = detector.detect(request);

            // if contentType == null, that means
            // that the detector was unavailable to detect content type
            if (contentType != null) {
                return contentType;
            }
        }

        // returning result
        // if nothing found, its safe to return null
        return null;
    }
}
