package com.cn.core.remote.andlinker;

import android.util.Log;

/**
 * Created by zl on 2019/8/15.
 * 主要用于AndLinker中的异常信息监听和保存
 */
public final class AndLinkerErrorHandler {

    private static OnAndLinkerErrorListener mOnAndLinkerErrorListener;

    private AndLinkerErrorHandler(){

    }

    static void printErr(String tag,String msg){
        printErr(tag,msg,null);
    }

    static void printErr(String tag,String msg,Throwable tr){
        String printMsg = makePrintMsg(msg,tr);
        if(mOnAndLinkerErrorListener!=null){
            mOnAndLinkerErrorListener.onAndLinkerError(tag,printMsg);
        }else{
            Log.e(tag,printMsg);
        }
    }

    //by zl 2019.08.15 补上详细的异常日志信息
    static String makePrintMsg(String baseResultMsg,Throwable throwable){
        String errMsg = Log.getStackTraceString(throwable);
        return baseResultMsg+errMsg;
    }

    public static void setOnAndLinkerErrorListener(OnAndLinkerErrorListener listener){
        mOnAndLinkerErrorListener = listener;
    }

    public interface OnAndLinkerErrorListener{
        void onAndLinkerError(String tag,String errorMsg);
    }
}
