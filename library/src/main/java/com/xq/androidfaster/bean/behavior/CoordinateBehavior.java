package com.xq.androidfaster.bean.behavior;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public interface CoordinateBehavior extends Serializable, Parcelable {

    @Override
    default int describeContents() {
        return 0;
    }

    @Override
    default void writeToParcel(Parcel dest, int flags) {

    }

    public void getX();

    public void getY();

    public void getZ();

    //上述内容在属性值中扮演的角色，不需要处理判断逻辑可直接返回null
    default String getCoordinateRole(){
        return null;
    }

}
