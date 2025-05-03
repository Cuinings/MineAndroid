package com.cn.library.remote.andlinker;

import android.os.Handler;
import android.os.Looper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 16:05
 * @Description:
 */
public class OriginalCallAdapterFactory extends CallAdapter.Factory {
    private Executor mCallbackExecutor;

    private OriginalCallAdapterFactory(Executor callbackExecutor) {
        this.mCallbackExecutor = callbackExecutor;
    }

    public static OriginalCallAdapterFactory create() {
        return new OriginalCallAdapterFactory(new MainThreadExecutor());
    }

    public static OriginalCallAdapterFactory create(Executor callbackExecutor) {
        return new OriginalCallAdapterFactory(callbackExecutor);
    }

    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations) {
        return getRawType(returnType) != Call.class ? null : new CallAdapter<Object, Call<?>>() {
            public Call<Object> adapt(Call<Object> call) {
                return new ExecutorCallbackCall(OriginalCallAdapterFactory.this.mCallbackExecutor, call);
            }
        };
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler handler;

        private MainThreadExecutor() {
            this.handler = new Handler(Looper.getMainLooper());
        }

        public void execute(Runnable r) {
            this.handler.post(r);
        }
    }

    static final class ExecutorCallbackCall<T> implements Call<T> {
        final Executor mCallbackExecutor;
        final Call<T> mDelegate;

        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
            this.mCallbackExecutor = callbackExecutor;
            this.mDelegate = delegate;
        }

        public T execute() {
            return this.mDelegate.execute();
        }

        public void enqueue(final Callback<T> callback) {
            if (callback == null) {
                throw new NullPointerException("callback == null");
            } else {
                this.mDelegate.enqueue(new Callback<T>() {
                    public void onResponse(Call<T> call, final T response) {
                        ExecutorCallbackCall.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                if (ExecutorCallbackCall.this.mDelegate.isCanceled()) {
                                    callback.onFailure(ExecutorCallbackCall.this, new IllegalStateException("Already canceled"));
                                } else {
                                    callback.onResponse(ExecutorCallbackCall.this, response);
                                }

                            }
                        });
                    }

                    public void onFailure(Call<T> call, final Throwable t) {
                        ExecutorCallbackCall.this.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                callback.onFailure(ExecutorCallbackCall.this, t);
                            }
                        });
                    }
                });
            }
        }

        public boolean isExecuted() {
            return this.mDelegate.isExecuted();
        }

        public void cancel() {
            this.mDelegate.cancel();
        }

        public boolean isCanceled() {
            return this.mDelegate.isCanceled();
        }
    }
}
