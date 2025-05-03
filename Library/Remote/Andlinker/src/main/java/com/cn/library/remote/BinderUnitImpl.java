package com.cn.library.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.cn.library.remote.andlinker.AndLinker;
import com.cn.library.remote.andlinker.AndLinkerUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 16:05
 * @Description:
 */
class BinderUnitImpl implements BinderUnit {
    private final String TAG;
    private final long REBIND_DELAYED;
    private Context mContext;
    private String mSvcPkgName;
    private String mAction;
    private AndLinker.BindCallback mRegBindCallback;
    private AndLinker.BindCallback mBindCallback;
    private AndLinker mAndLinker;
    private HashMap<String, Object> mRemoteApiObjCache;
    private List<Object> mCbkCache = new ArrayList<>();
    private boolean isActiveUnbind;
    private boolean isBind;
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 100 && !BinderUnitImpl.this.mAndLinker.isBind()) {
                BinderUnitImpl.this.bind();
            }
        }
    };;

    public BinderUnitImpl(Context context, String svcPkgName, String action) {
        this(context, svcPkgName, action, (AndLinker.BindCallback)null);
    }

    public BinderUnitImpl(Context context, String svcPkgName,  String action, AndLinker.BindCallback regBindCallback) {
        this.TAG = "BinderUnitImpl";
        this.REBIND_DELAYED = 3000L;
        this.mRemoteApiObjCache = new HashMap();
        this.isActiveUnbind = false;
        this.isBind = false;
        this.mContext = context;
        this.mSvcPkgName = svcPkgName;
        this.mAction = action;
        this.mRegBindCallback = regBindCallback;
        this.mBindCallback = this.newBindCallback();
        this.init();
    }

    public void bind() {
        this.isActiveUnbind = false;
        if (this.mAndLinker.isBind()) {
        } else {
            this.mAndLinker.bind();
            this.checkRebind();
        }
    }

    public void unbind() {
        this.isActiveUnbind = true;
        if (!this.mAndLinker.isBind()) {
        } else {
            this.mAndLinker.unbind();
            this.rmvCheckRebind();
        }
    }

    public boolean isBind() {
        return this.isBind;
    }

    public <T> T createRemoteApiInstance(Class<T> api) {
        T t = null;
        if (this.mRemoteApiObjCache.containsKey(api.getName())) {
            Object obj = this.mRemoteApiObjCache.get(api.getName());
            t = obj == null ? null : (T) obj;
        }

        if (t != null) {
            return t;
        } else {
            t = this.mAndLinker.create(api);
            this.mRemoteApiObjCache.put(api.getName(), t);
            return t;
        }
    }

    public <T> T getRmtApiFromCache(Class<T> api) {
        return this.mRemoteApiObjCache.containsKey(api.getName()) ? (T) this.mRemoteApiObjCache.get(api.getName()) : null;
    }

    public void registerCallback(Object obj) {
        if (!this.mCbkCache.contains(obj)) {
            this.mAndLinker.registerObject(obj);
            this.mCbkCache.add(obj);
            Log.d(TAG, "registerCallback: " + obj);
        }
    }

    public void unRegisterCallback(Object obj) {
        if (this.mCbkCache.contains(obj)) {
            this.mAndLinker.unRegisterObject(obj);
            this.mCbkCache.remove(obj);
        }
    }

    public void registerBinderCallback(AndLinker.BindCallback callback) {
        this.mBindCallback = callback;
    }

    public void unRegisterBinderCallback(AndLinker.BindCallback callback) {
        this.mBindCallback = null;
    }

    private void init() {
        this.mAndLinker = AndLinkerUtils.buildAndLinker(this.mContext, this.mSvcPkgName, this.mAction, this.mBindCallback);
    }

    private AndLinker.BindCallback newBindCallback() {
        return new AndLinker.BindCallback() {
            public void onBind() {
                BinderUnitImpl.this.isBind = true;
                BinderUnitImpl.this.rmvCheckRebind();
                if (BinderUnitImpl.this.mRegBindCallback != null) {
                    BinderUnitImpl.this.mRegBindCallback.onBind();
                }

            }

            public void onUnBind() {
                if (BinderUnitImpl.this.isBind && BinderUnitImpl.this.mRegBindCallback != null) {
                    BinderUnitImpl.this.mRegBindCallback.onUnBind();
                }

                BinderUnitImpl.this.isBind = false;
                BinderUnitImpl.this.checkRebind();
            }
        };
    }

    private void checkRebind() {
        if (!this.isActiveUnbind) {
            this.rmvCheckRebind();
            this.handler.sendEmptyMessageDelayed(100, 3000L);
        }
    }

    private void rmvCheckRebind() {
        this.handler.removeCallbacksAndMessages((Object)null);
        if (this.handler.hasMessages(100)) {
            this.handler.removeCallbacksAndMessages((Object)null);
        }

        if (this.handler.hasMessages(100)) {
            this.rmvCheckRebind();
        }

    }
}
