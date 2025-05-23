package com.cn.library.remote.andlinker;

import android.os.Binder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.cn.library.remote.andlinker.annotation.RemoteInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by codezjx on 2017/10/3.<br/>
 */
final class Invoker {

    private static final String TAG = "Invoker";

    private final ConcurrentHashMap<String, Class<?>> mCallbackClassTypes;
    private final ConcurrentHashMap<String, MethodExecutor> mMethodExecutors;
    private final RemoteCallbackList<ICallback> mCallbackList;


    Invoker() {
        mCallbackClassTypes = new ConcurrentHashMap<String, Class<?>>();
        mMethodExecutors = new ConcurrentHashMap<String, MethodExecutor>();
        mCallbackList = new RemoteCallbackList<ICallback>();
    }

    private void handleCallbackClass(Class<?> clazz, boolean isRegister) {
        if (!clazz.isAnnotationPresent(RemoteInterface.class)) {
            throw new IllegalArgumentException("Callback interface doesn't has @RemoteInterface annotation.");
        }
        String className = clazz.getSimpleName();
        if (isRegister) {
            mCallbackClassTypes.putIfAbsent(className, clazz);
        } else {
            mCallbackClassTypes.remove(className);
        }
    }

    private void handleObject(Object target, boolean isRegister) {
        if (target == null) {
            throw new NullPointerException("Object to (un)register must not be null.");
        }
        Class<?>[] interfaces = target.getClass().getInterfaces();

        //by zl 20200701， 这块我改成了继承Class 而非接口，所以这边改下，如果拿不到实现接口，就获取父类的接口
        if(interfaces == null||interfaces.length==0) interfaces = target.getClass().getSuperclass().getInterfaces();

        if (interfaces.length != 1) {
            throw new IllegalArgumentException("Remote object must extend just one interface.");
        }
        Class<?> clazz = interfaces[0];
        if (!clazz.isAnnotationPresent(RemoteInterface.class)) {
            throw new IllegalArgumentException("Interface doesn't has @RemoteInterface annotation.");
        }
        // Cache all annotation method
        String clsName = clazz.getSimpleName();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            // The compiler sometimes creates synthetic bridge methods as part of the
            // type erasure process. As of JDK8 these methods now include the same
            // annotations as the original declarations. They should be ignored for
            // subscribe/produce.
            if (method.isBridge()) {
                continue;
            }
            String methodName = method.getName();
            String key = createMethodExecutorKey(clsName, methodName);
            if (isRegister) {
                MethodExecutor executor = new MethodExecutor(target, method);
                MethodExecutor preExecutor = mMethodExecutors.putIfAbsent(key, executor);
                if (preExecutor != null) {
                    throw new IllegalStateException("Key conflict with class:" + clsName + " method:" + methodName
                            + ". Please try another class/method name.");
                }
            } else {
                mMethodExecutors.remove(key);
            }
            // Cache callback class if exist
            Class<?>[] paramCls = method.getParameterTypes();
            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < paramCls.length; i++) {
                Class<?> cls = paramCls[i];
                Annotation[] annotations = paramAnnotations[i];
                for (Annotation annotation : annotations) {
                    if (annotation instanceof com.cn.library.remote.andlinker.annotation.Callback) {
                        handleCallbackClass(cls, isRegister);
                    }
                }
            }
        }
    }

    private boolean containCallbackAnnotation(Annotation[] annotations) {
        boolean result = false;
        if (null != annotations && annotations.length != 0) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof com.cn.library.remote.andlinker.annotation.Callback) {
                    result = true;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    void registerObject(Object target) {
        handleObject(target, true);
    }

    void unRegisterObject(Object target) {
        handleObject(target, false);
    }

    Response invoke(Request request) {
        Object[] args = handlerArgs(request.getArgsWrapper());
        MethodExecutor executor = getMethodExecutor(request);
        if (executor == null) {
            String errMsg = String.format("The method '%s' you call was not exist!", request.getMethodName());
            return new Response(Response.STATUS_CODE_NOT_FOUND, errMsg, null);
        }
        return executor.execute(args);
    }


    private Object[] handlerArgs(BaseTypeWrapper[] wrappers) {
        if (wrappers == null || wrappers.length == 0)
            return null;
        Object[] args = new Object[wrappers.length];
        for (int i = 0; i < wrappers.length; i++) {
            // Assign the origin args parameter
            args[i] = wrappers[i].getParam();
            if (wrappers[i].getType() == BaseTypeWrapper.TYPE_CALLBACK) {
                int pid = Binder.getCallingPid();
                String clazzName = ((CallbackTypeWrapper) wrappers[i]).getClassName();
                Class<?> clazz = getCallbackClass(clazzName);
                if (clazz == null) {
                    throw new IllegalStateException("Can't find callback class: " + clazzName);
                }
                //把对象作为参数
                args[i] = getCallbackProxy(clazz, pid);
            }
        }
        return args;
    }

    RemoteCallbackList<ICallback> getCallbackList() {
        return mCallbackList;
    }

    @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
    private <T> T getCallbackProxy(final Class<T> service, final int pid) {
        Utils.validateServiceInterface(service);
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        /**
                         *
                         * 在多线程中回调该接口可能会出现
                         * java.lang.IllegalStateException: beginBroadcast() called while already in a broadcast
                         *
                         * */
                        synchronized (mCallbackList) {
                            Object result = null;
                            //添加策略，解决由于回调出现一次异常导致后续所有接口调用均不可用
                            int len = -1;
                            try {
                                len = mCallbackList.beginBroadcast();
                                for (int i = 0; i < len; i++) {
                                    int cookiePid = (int) mCallbackList.getBroadcastCookie(i);
//                                    Log.d("Invoker", "getCallbackProxy---->cookiePid = " + cookiePid);
                                    if (cookiePid == pid) {
                                        try {
                                            Request request = createCallbackRequest(service.getSimpleName(), method.getName(), args);
                                            Response response = mCallbackList.getBroadcastItem(i).callback(request);
                                            result = response.getResult();
                                            if (response.getStatusCode() != Response.STATUS_CODE_SUCCESS) {
                                                Logger.e(TAG, "Execute remote callback fail: " + response.toString());
                                            }
                                        } catch (RemoteException e) {
                                            Logger.e(TAG, "Error when execute callback!", e);
                                        }
                                        break;
                                    }
                                }
                            }finally {
                                if(len!=-1)
                                    mCallbackList.finishBroadcast();
                            }
                            return result;
                        }
                    }
                });
    }

    private Request createCallbackRequest(String targetClass, String methodName, Object[] args) {
        Logger.i(TAG, "===createCallbackRequest===> targetClass " + targetClass + " methodName" + methodName + "args" + args);
        BaseTypeWrapper[] wrappers = handleTypeWrapper(args);
        return new Request(targetClass, methodName, wrappers);
    }

    /**
     * 处理callBack 的 param 可能存在null
     *
     * @param args
     * @return
     */
    private BaseTypeWrapper[] handleTypeWrapper(Object[] args) {
        if (args == null || args.length == 0)
            return null;
        BaseTypeWrapper[] wrappers = new BaseTypeWrapper[args.length];
        for (int i = 0; i < args.length; i++) {
            wrappers[i] = new InTypeWrapper(args[i], args[i].getClass());
        }

        return wrappers;
    }

    private Class<?> getCallbackClass(String className) {
        return mCallbackClassTypes.get(className);
    }

    private String createMethodExecutorKey(String clsName, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append(clsName)
                .append('-')
                .append(methodName);
        return sb.toString();
    }

    private MethodExecutor getMethodExecutor(Request request) {
        String key = createMethodExecutorKey(request.getTargetClass(), request.getMethodName());
        return mMethodExecutors.get(key);
    }

}
