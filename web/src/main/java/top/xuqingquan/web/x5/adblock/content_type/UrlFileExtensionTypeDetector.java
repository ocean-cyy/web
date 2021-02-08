package top.xuqingquan.web.x5.adblock.content_type;

import android.os.Build;
import androidx.annotation.RequiresApi;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;

import org.adblockplus.libadblockplus.FilterEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 许清泉 on 11/28/20 1:38 AM
 */
public class UrlFileExtensionTypeDetector implements ContentTypeDetector {
    private static final String[] EXTENSIONS_JS = {"js"};
    private static final String[] EXTENSIONS_CSS = {"css"};
    private static final String[] EXTENSIONS_FONT = {"ttf", "woff", "woff2"};
    private static final String[] EXTENSIONS_HTML = {"htm", "html"};
    // listed https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types
    private static final String[] EXTENSIONS_IMAGE = {"gif", "png", "jpg", "jpe", "jpeg", "bmp",
            "apng", "cur", "jfif", "ico", "pjpeg", "pjp", "svg", "tif", "tiff", "webp"};
    // video files listed here https://en.wikipedia.org/wiki/Video_file_format
    // audio files listed here https://en.wikipedia.org/wiki/Audio_file_format
    private static final String[] EXTENSIONS_MEDIA = {"webm", "mkv", "flv", "vob", "ogv",
            "drc", "mng", "avi", "mov", "gifv", "qt", "wmv", "yuv", "rm", "rmvb", "asf", "amv", "mp4",
            "m4p", "mp2", "mpe", "mpv", "mpg", "mpeg", "m2v", "m4v", "svi", "3gp", "3g2", "mxf", "roq",
            "nsv", "8svx", "aa", "aac", "aax", "act", "aiff", "alac", "amr", "ape", "au", "awb",
            "cda", "dct", "dss", "dvf", "flac", "gsm", "iklax", "ivs", "m4a", "m4b", "mmf", "mogg",
            "mp3", "mpc", "msv", "nmf", "oga", "ogg", "opus", "ra", "raw", "rf64", "sln", "tta",
            "voc", "vox", "wav", "wma", "wv"};

    private static final Map<String, FilterEngine.ContentType> extensionTypeMap
            = new HashMap<String, FilterEngine.ContentType>();

    private static void mapExtensions(
            final String[] extensions,
            final FilterEngine.ContentType contentType) {
        for (final String extension : extensions) {
            // all comparisons are in lower case, force that the extensions are in lower case
            extensionTypeMap.put(extension.toLowerCase(), contentType);
        }
    }

    static {
        mapExtensions(EXTENSIONS_JS, FilterEngine.ContentType.SCRIPT);
        mapExtensions(EXTENSIONS_CSS, FilterEngine.ContentType.STYLESHEET);
        mapExtensions(EXTENSIONS_FONT, FilterEngine.ContentType.FONT);
        mapExtensions(EXTENSIONS_HTML, FilterEngine.ContentType.SUBDOCUMENT);
        mapExtensions(EXTENSIONS_IMAGE, FilterEngine.ContentType.IMAGE);
        mapExtensions(EXTENSIONS_MEDIA, FilterEngine.ContentType.MEDIA);
    }

    // JavaDoc inherited from base interface
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public FilterEngine.ContentType detect(final WebResourceRequest request) {
        if (request == null || request.getUrl() == null) {
            return null;
        }
        final String path = request.getUrl().getPath();
        if (path == null) {
            return null;
        }
        final int lastIndexOfDot = path.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return null;
        }
        final String fileExtension = path.substring(lastIndexOfDot + 1);
        if (fileExtension != null) {
            return extensionTypeMap.get(fileExtension.toLowerCase());
        }
        return null;
    }
}
