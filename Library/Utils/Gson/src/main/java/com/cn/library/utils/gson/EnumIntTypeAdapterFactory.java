package com.cn.library.utils.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public final class EnumIntTypeAdapterFactory implements TypeAdapterFactory {

    public static EnumIntTypeAdapterFactory create() {
        return new EnumIntTypeAdapterFactory();
    }

    private EnumIntTypeAdapterFactory() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (rawType == null || rawType == Enum.class) {
            return null;
        }
        if (!Enum.class.isAssignableFrom(rawType)) {
            return null;
        }
        if (!rawType.isEnum()) {
            return null;
        }

        if (rawType.getAnnotation(IgnoreEnumIntConvert.class) != null) {
            return null;
        }
        return (TypeAdapter<T>) new EnumTypeAdapter(rawType);
    }
}