package com.cn.core.remote.andlinker;

import android.content.Context;

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 16:05
 * @Description:
 */
public final class AndLinkerUtils {
    public static AndLinker buildAndLinker(Context context, String svcAppPkgName, String action) {
        return buildAndLinker(context, svcAppPkgName, action, (AndLinker.BindCallback)null);
    }

    public static AndLinker buildAndLinker(Context context, String svcAppPkgName, String action, AndLinker.BindCallback bindCallback) {
        AndLinker mLinker = null;
        if (mLinker != null) {
            return mLinker;
        } else {
            mLinker = (new AndLinker.Builder(context)).packageName(svcAppPkgName).action(action).addCallAdapterFactory(OriginalCallAdapterFactory.create()).build();
            if (bindCallback != null) {
                mLinker.setBindCallback(bindCallback);
            }

            return mLinker;
        }
    }

    private AndLinkerUtils() {
    }

    static {
        Logger.setLogAdapter((i, tag, msg) -> {
        });
        AndLinkerErrorHandler.setOnAndLinkerErrorListener((tag, msg) -> {
        });
    }
}
