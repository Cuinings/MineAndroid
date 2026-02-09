package com.cn.core.remote.andlinker.adapter;



import com.cn.core.remote.andlinker.Call;
import com.cn.core.remote.andlinker.CallAdapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Default {@linkplain CallAdapter.Factory call adapter} which adapt {@link Call} to the execute result.
 */
public class DefaultCallAdapterFactory extends CallAdapter.Factory {

    public static final CallAdapter.Factory INSTANCE = new DefaultCallAdapterFactory();

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations) {
        return new CallAdapter<Object, Object>() {
            @Override
            public Object adapt(Call<Object> call) {
                // Return the result
                return call.execute();
            }
        };
    }
    
}
