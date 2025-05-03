package com.cn.library.remote;

import android.content.Context;

import com.cn.library.remote.andlinker.AndLinker;

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 16:04
 * @Description:
 */
public interface BinderUnit {
    void bind();

    void unbind();

    boolean isBind();

    <T> T createRemoteApiInstance(Class<T> var1);

    <T> T getRmtApiFromCache(Class<T> var1);

    void registerCallback(Object var1);

    void unRegisterCallback(Object var1);

    void registerBinderCallback(AndLinker.BindCallback var1);

    void unRegisterBinderCallback(AndLinker.BindCallback var1);

    public static final class Factory {
        private Factory() {
        }

        public static BinderUnit newBinderUnit(Context context, String svcPkgName, String action, AndLinker.BindCallback regBindCallback) {
            return new BinderUnitImpl(context, svcPkgName, action, regBindCallback);
        }
    }
}
