package com.cn.library.andlinker;

import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by codezjx on 2017/9/13.<br/>
 */
final class LinkerBinderImpl extends ITransfer.Stub implements AndLinkerBinder {
    
    private static final String TAG = "LinkerBinder";
    private RemoteCallbackList<ICallback> mCallbackList;
    private Invoker mInvoker;

    LinkerBinderImpl() {
        mInvoker = new Invoker();
        mCallbackList = mInvoker.getCallbackList();
    }

    @Override
    public void registerObject(Object target) {
        mInvoker.registerObject(target);
    }

    @Override
    public void unRegisterObject(Object target) {
        mInvoker.unRegisterObject(target);
    }
    
    @Override
    public Response execute(Request request) {
        for (BaseTypeWrapper wrapper : request.getArgsWrapper()) {
            Logger.d(TAG, "Receive param, value:" + wrapper.getParam() + " type:" + (wrapper.getParam() != null ? wrapper.getParam().getClass() : "null"));
        }
        Logger.d(TAG, "Receive request:" + request.getMethodName());
        return mInvoker.invoke(request);
    }

    @Override
    public void register(ICallback callback) {
        int pid = Binder.getCallingPid();
        Logger.d(TAG, "register callback:" + callback + " pid:" + pid);
        if (callback != null) {
            mCallbackList.register(callback, pid);
        }
    }

    @Override
    public void unRegister(ICallback callback) {
        int pid = Binder.getCallingPid();
        Logger.d(TAG, "unRegister callback:" + callback + " pid:" + pid);
        if (callback != null) {
            mCallbackList.unregister(callback);
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        boolean result = false;
        try{
            result = super.onTransact(code, data, reply, flags);
        }catch(Exception e){
            Log.e("LinkerBinderImpl","Unexpected remote exception",e);
            throw e;
        }
        return result;
    }
}
