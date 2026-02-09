package com.cn.core.remote.msg.router.client.bean

import android.annotation.SuppressLint
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.cn.core.remote.andlinker.SuperParcelable

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 14:49
 * @Description:
 */
data class Msg(
    var source: String = "",
    var target: String = "",
    var content: MsgBody = MsgBody()
) : Parcelable, SuperParcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()?:"",
        parcel.readString()?:"",
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(MsgBody::class.java.classLoader, MsgBody::class.java)
        } else {
            parcel.readParcelable(MsgBody::class.java.classLoader)
        })?: MsgBody()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeString(source)
        p0.writeString(target)
        p0.writeParcelable(content, p1)
    }

    @SuppressLint("NewApi")
    override fun readFromParcel(parcel: Parcel?) {
        source = parcel?.readString()?:""
        target = parcel?.readString()?:""
        content = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel?.readParcelable(MsgBody::class.java.classLoader, MsgBody::class.java)
        } else {
            parcel?.readParcelable(MsgBody::class.java.classLoader)
        })?: MsgBody()
    }

    companion object CREATOR : Parcelable.Creator<Msg> {
        override fun createFromParcel(parcel: Parcel): Msg {
            return Msg(parcel)
        }

        override fun newArray(size: Int): Array<Msg?> {
            return arrayOfNulls(size)
        }
    }
}