package top.xuqingquan.web.nokernel

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by 许清泉 on 12/21/20 11:45 PM
 */
class Action() : Parcelable {

    var permissions: List<String>? = null
    var action = 0
    var fromIntention = 0

    constructor(parcel: Parcel) : this() {
        permissions = parcel.createStringArrayList()
        action = parcel.readInt()
        fromIntention = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringList(permissions)
        parcel.writeInt(action)
        parcel.writeInt(fromIntention)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Transient
        const val ACTION_PERMISSION = 1

        @Transient
        const val ACTION_FILE = 2

        @Transient
        const val ACTION_CAMERA = 3

        @Transient
        const val ACTION_VIDEO = 4

        @JvmStatic
        fun createPermissionsAction(permissions: List<String>): Action {
            val mAction = Action()
            mAction.action = ACTION_PERMISSION
            mAction.permissions = permissions
            return mAction
        }

        @Suppress("unused")
        @JvmStatic
        val CREATOR = object : Parcelable.Creator<Action> {
            override fun createFromParcel(parcel: Parcel): Action {
                return Action(parcel)
            }

            override fun newArray(size: Int): Array<Action?> {
                return arrayOfNulls(size)
            }
        }
    }
}