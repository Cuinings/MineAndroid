package com.cn.library.remote.andlinker;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by codezjx on 2017/10/22.<br/>
 */
final class MethodExecutor {
    
    /** Target object to invoke. */
    private final Object mTarget;
    /** Method to execute. */
    private final Method mMethod;

    MethodExecutor(Object target, Method method) {
        if (target == null) {
            throw new NullPointerException("Target cannot be null.");
        }
        if (method == null) {
            throw new NullPointerException("Method cannot be null.");
        }

        mTarget = target;
        mMethod = method;
        method.setAccessible(true);
    }

    Response execute(Object[] args) {
        Object result = null;
        int statusCode = Response.STATUS_CODE_SUCCESS;
        String resultMsg = String.format("Call method '%s' successfully!", mMethod.getName());
        Throwable throwable = null;
        try {
            result = mMethod.invoke(mTarget, args);
        } catch (IllegalAccessException e) {
            statusCode = Response.STATUS_CODE_ILLEGAL_ACCESS;
            throwable = e;
        } catch (InvocationTargetException e) {
            statusCode = Response.STATUS_CODE_INVOCATION_FAIL;
            throwable = e.getTargetException();
        } finally {
            if (throwable != null) {
                String className = mTarget!=null?mTarget.getClass().getSimpleName():"";
                resultMsg = "Exception occur when execute AndLinkerServer method:" + className+"."+mMethod.getName()+"()" + '\n' + throwable.getMessage();
                AndLinkerErrorHandler.printErr("MethodExecutor",resultMsg);
            } else {
                resultMsg = AndLinkerErrorHandler.makePrintMsg(resultMsg, throwable);
//                Log.d("MethodExecutor", resultMsg);
            }
            return new Response(statusCode, resultMsg, result);
        }
    }
}
