package com.cn.core.remote.msg.router.client.bean

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.view.Surface
import com.cn.core.remote.andlinker.SuperParcelable

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 15:13
 * @Description:
 */

data class MsgBody(
    var code: String? = null,
    var body: String? = null,
    var surface: Surface? = null
) : Parcelable, SuperParcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Surface::class.java.classLoader, Surface::class.java)
        } else {
            parcel.readParcelable(Surface::class.java.classLoader)
        }
    ) {
    }

    override fun readFromParcel(parcel: Parcel?) {
        code = parcel?.readString()
        body = parcel?.readString()
        surface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel?.readParcelable(Surface::class.java.classLoader, Surface::class.java)
        } else {
            parcel?.readParcelable(Surface::class.java.classLoader)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(code)
        parcel.writeString(body)
        parcel.writeParcelable(surface, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MsgBody> {
        override fun createFromParcel(parcel: Parcel): MsgBody {
            return MsgBody(parcel)
        }

        override fun newArray(size: Int): Array<MsgBody?> {
            return arrayOfNulls(size)
        }
    }
}
