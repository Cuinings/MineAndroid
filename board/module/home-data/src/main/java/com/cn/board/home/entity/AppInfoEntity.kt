package com.cn.board.home.entity

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_table")
data class AppInfoEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var clazz: String = "",
    var packageName: String = "",
    var versionCode: String = "",
    var versionName: String = "",
    var appType: EmAppType = EmAppType.Third,
    var main: Int = 0,
    var name: String? = null,
    var allowDelete: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    var mainIndex: Int = -1,

    var offlineMain: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var offlineMainIndex: Int = -1,
): Parcelable {

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        clazz = parcel.readString().toString()
        packageName = parcel.readString().toString()
        versionCode = parcel.readString().toString()
        versionName = parcel.readString().toString()
        main = parcel.readInt()
        allowDelete = parcel.readByte() != 0.toByte()
        mainIndex = parcel.readInt()
        offlineMain = parcel.readInt()
        offlineMainIndex = parcel.readInt()
    }

    fun copy(info: AppInfoEntity) {
        this.id = info.id
        this.clazz = info.clazz
        this.packageName = info.packageName
        this.versionCode = info.versionCode
        this.versionName = info.versionName
        this.appType = info.appType
        this.main = info.main
        this.offlineMain = info.offlineMain
        this.offlineMainIndex = info.offlineMainIndex
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(clazz)
        parcel.writeString(packageName)
        parcel.writeString(versionCode)
        parcel.writeString(versionName)
        parcel.writeInt(main)
        parcel.writeByte(if (allowDelete) 1 else 0)
        parcel.writeInt(mainIndex)
        parcel.writeInt(offlineMain)
        parcel.writeInt(offlineMainIndex)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AppInfoEntity> {
        override fun createFromParcel(parcel: Parcel): AppInfoEntity {
            return AppInfoEntity(parcel)
        }

        override fun newArray(size: Int): Array<AppInfoEntity?> {
            return arrayOfNulls(size)
        }
    }

}
