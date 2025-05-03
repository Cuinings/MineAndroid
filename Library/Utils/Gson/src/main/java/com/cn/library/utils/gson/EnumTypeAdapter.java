package com.cn.library.utils.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

final class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Map<Integer, T> nameToConstant = new HashMap<>();
    private final Map<T, Integer> constantToName = new HashMap<>();

    public EnumTypeAdapter(Class<T> classOfT) {
        T[] enumConstants = classOfT.getEnumConstants();
        if (enumConstants == null) {
            throw new NullPointerException(classOfT.getName() + ".getEnumConstants() == null");
        }
        for (T constant : enumConstants) {
            String name = constant.name();
            Field field;
            try {
                field = classOfT.getField(name);
            } catch (NoSuchFieldException e) {
                throw new AssertionError(e);
            }
            SerializedName annotation = field.getAnnotation(SerializedName.class);
            if (annotation == null) {
                throw new IllegalArgumentException("Enum class Field must Annotation with SerializedName：" + classOfT.getName() + "." + name);
            }
            String value = annotation.value();
            Integer intValue;
            try {
                intValue = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Enum class Field must Annotation with SerializedName And value is int type current is："
                        + value + "\n\t\tin " + classOfT.getName() + "." + name, e);
            }
            T previous = nameToConstant.put(intValue, constant);
            if (previous != null) {
                throw new IllegalArgumentException("Enum class fields are repeatedly identified by the serializedName annotation：" +
                        "\n\t\tserializedName = " + intValue + " And two enum are" +
                        "\n\t\t1." + classOfT.getName() + "." + previous.name() +
                        "\n\t\t2." + classOfT.getName() + "." + constant.name());
            }
            constantToName.put(constant, intValue);
        }
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return nameToConstant.get(in.nextInt());
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.value(value == null ? null : constantToName.get(value));
    }
}