package top.xuqingquan.web.nokernel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.os.EnvironmentCompat
import android.text.TextUtils
import android.widget.Toast
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.getMIMEType
import top.xuqingquan.web.R
import top.xuqingquan.web.nokernel.WebConfig.AGENTWEB_CACHE_PATCH
import top.xuqingquan.web.nokernel.WebConfig.AGENTWEB_FILE_PATH
import top.xuqingquan.web.nokernel.WebConfig.FILE_CACHE_PATH
import top.xuqingquan.web.publics.WebParentLayout
import top.xuqingquan.web.provider.ScaffoldWebFileProvider
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by 许清泉 on 2019-06-19 21:55
 */
object WebUtils {

    private var mToast: Toast? = null

    private var mHandler: Handler? = null

    /**
     * @param context
     * @return WebView 的缓存路径
     */
    @JvmStatic
    fun getCachePath(context: Context): String {
        return context.cacheDir.absolutePath + AGENTWEB_CACHE_PATCH
    }

    @JvmStatic
    fun getAgentWebFilePath(context: Context): String? {
        if (!TextUtils.isEmpty(AGENTWEB_FILE_PATH)) {
            return AGENTWEB_FILE_PATH
        }
        val dir = getDiskExternalCacheDir(context)
        val mFile = File(dir, FILE_CACHE_PATH)
        try {
            if (!mFile.exists()) {

                mFile.mkdirs()
            }
        } catch (throwable: Throwable) {
            Timber.i("create dir exception")
        }

        Timber.i("path:" + mFile.absolutePath + "  path:" + mFile.path)
        AGENTWEB_FILE_PATH = mFile.absolutePath
        return AGENTWEB_FILE_PATH
    }

    @JvmStatic
    internal fun getDiskExternalCacheDir(context: Context): String? {
        val mFile = context.externalCacheDir
        return if (mFile != null && Environment.MEDIA_MOUNTED == EnvironmentCompat.getStorageState(mFile)) {
            mFile.absolutePath
        } else null
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createFileByName(context: Context, name: String, cover: Boolean): File? {
        val path = getAgentWebFilePath(context)
        if (path.isNullOrEmpty()) {
            return null
        }
        val mFile = File(path, name)
        if (mFile.exists()) {
            if (cover) {
                mFile.delete()
                mFile.createNewFile()
            }
        } else {
            mFile.createNewFile()
        }
        return mFile
    }

    @JvmStatic
    fun createImageFile(context: Context): File? {
        var mFile: File? = null
        try {
            val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val imageName = "aw_$timeStamp.jpg"
            mFile = createFileByName(context, imageName, true)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return mFile
    }

    @JvmStatic
    fun createVideoFile(context: Context): File? {
        var mFile: File? = null
        try {
            val timeStamp: String? =
                SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                    .format(Date())
            val imageName = "aw_$timeStamp.mp4"  //默认生成mp4
            mFile = createFileByName(context, imageName, true)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return mFile
    }

    @JvmStatic
    fun getIntentCaptureCompat(context: Context, file: File): Intent {
        val mIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val mUri = getUriFromFile(context, file)
        mIntent.addCategory(Intent.CATEGORY_DEFAULT)
        mIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
        return mIntent
    }

    @JvmStatic
    fun getIntentVideoCompat(context: Context, file: File): Intent {
        val mIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        val mUri = getUriFromFile(context, file)
        mIntent.addCategory(Intent.CATEGORY_DEFAULT)
        mIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
        return mIntent
    }

    @JvmStatic
    fun getCommonFileIntentCompat(context: Context, file: File): Intent {
        val mIntent = Intent().setAction(Intent.ACTION_VIEW)
        setIntentDataAndType(context, mIntent, getMIMEType(file), file, false)
        return mIntent
    }

    private fun setIntentDataAndType(
        context: Context,
        intent: Intent,
        type: String,
        file: File,
        writeAble: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setDataAndType(getUriFromFile(context, file), type)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (writeAble) {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        } else {
            intent.setDataAndType(Uri.fromFile(file), type)
        }
    }

    @SuppressLint("ShowToast")
    @JvmStatic
    fun toastShowShort(context: Context, msg: String) {
        if (mToast == null) {
            mToast = Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT)
        } else {
            mToast!!.setText(msg)
        }
        mToast!!.show()
    }

    @JvmStatic
    @Deprecated("")
    internal fun getUIControllerAndShowMessage(activity: Activity?, message: String, from: String) {
        if (activity == null || activity.isFinishing) {
            return
        }
        val mWebParentLayout = activity.findViewById<WebParentLayout>(R.id.scaffold_web_parent_layout_id)
        val mAgentWebUIController = mWebParentLayout.provide()
        mAgentWebUIController?.onShowMessage(message, from)
    }

    @JvmStatic
    fun isUIThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    @JvmStatic
    fun runInUiThread(runnable: Runnable) {
        if (mHandler == null) {
            mHandler = Handler(Looper.getMainLooper())
        }
        mHandler!!.post(runnable)
    }

    @JvmStatic
    fun isExistMethod(o: Any?, methodName: String, vararg clazzs: Class<*>): Method? {
        if (null == o) {
            return null
        }
        try {
            val clazz = o.javaClass
            val mMethod = clazz.getDeclaredMethod(methodName, *clazzs)
            mMethod.isAccessible = true
            return mMethod
        } catch (throwable: Throwable) {
            Timber.e(throwable)
        }
        return null
    }

    @JvmStatic
    fun getUriFromFile(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ScaffoldWebFileProvider.getUriForFile(context, context.packageName + ".ScaffoldWebFileProvider", file)
        } else {
            Uri.fromFile(file)
        }
    }
}
