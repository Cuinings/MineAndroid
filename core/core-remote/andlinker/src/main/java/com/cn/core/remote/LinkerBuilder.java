package com.cn.core.remote;

import android.content.Context;
import android.util.Log;

import com.cn.core.remote.andlinker.AndLinker;

import java.util.ArrayList;

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 16:04
 * @Description:
 */
public class LinkerBuilder {
    protected Context context;
    protected String action;
    protected String svrPkg;
    protected RemoteBindCallback callback;
    private AndLinker.BindCallback bindCallback = new AndLinker.BindCallback() {
        @Override
        public void onBind() {
            callback.bindResult(true);
        }

        @Override
        public void onUnBind() {
            callback.bindResult(false);
        }
    };
    protected ArrayList<Class> createInstanceList = new ArrayList();
    protected ArrayList<Object> registerObjList = new ArrayList();

    public LinkerBuilder(Context context) {
        this.context = context;
    }

    public LinkerBuilder svrPkg(String svrPkg) {
        this.svrPkg = svrPkg;
        return this;
    }

    public LinkerBuilder action(String action) {
        this.action = action;
        return this;
    }

    public LinkerBuilder createInstance(Class cls) {
        this.createInstanceList.add(cls);
        return this;
    }

    public LinkerBuilder registerCallback(Object obj) {
        this.registerObjList.add(obj);
        return this;
    }

    public LinkerBuilder bindCallback(RemoteBindCallback bindCallback) {
        this.callback = bindCallback;
        return this;
    }

    public BinderUnit build() {
        BinderUnit binderUnit = BinderUnit.Factory.newBinderUnit(this.context, this.svrPkg, this.action, this.bindCallback);
        int count = this.createInstanceList.size();

        int i;
        for(i = 0; i < count; ++i) {
            binderUnit.createRemoteApiInstance(this.createInstanceList.get(i));
        }

        count = this.registerObjList.size();

        for(i = 0; i < count; ++i) {
            binderUnit.registerCallback(this.registerObjList.get(i));
        }
        binderUnit.bind();
        this.createInstanceList.clear();
        this.createInstanceList = null;
        this.registerObjList.clear();
        this.registerObjList = null;
        return binderUnit;
    }

    private void debug() {
        Log.e("LinkerBuilder", "----------debug----------");
        Log.e("LinkerBuilder", "action:" + this.action);
        Log.e("LinkerBuilder", "createInstanceList.size:" + this.createInstanceList.size());
        Log.e("LinkerBuilder", "registerObjList.size:" + this.registerObjList.size());
    }
}
