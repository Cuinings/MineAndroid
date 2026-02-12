package com.cn.core.remote.msg.subscriber.processor

import com.cn.core.remote.msg.subscriber.annotation.BasicSubscriber
import com.cn.core.remote.msg.subscriber.processor.FilerExt.generate
import com.cn.core.remote.msg.subscriber.processor.SubscriberUtil.classQualifiedNameMap
import com.cn.core.remote.msg.subscriber.processor.SubscriberUtil.methodParameterTypeMap
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

/**
 * @Author: CuiNing
 * @Time: 2024/12/25 14:24
 * @Description:
 */
object SubscriberGenerate {

    fun Filer.generateSubscriber(println: (String) -> Unit) {
        classQualifiedNameMap.forEach { (className, qualifiedName) ->
            println("class:$qualifiedName.$className")
            val mClassName = "${className}Sub"
            val mClassTypeName = ClassName.get(qualifiedName, mClassName)
            //单例字段
            val instanceFieldSpec: FieldSpec = FieldSpec.builder(
                mClassTypeName, "instance",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.VOLATILE
            ).build()
            TypeSpec.classBuilder(mClassName).addModifiers(Modifier.PUBLIC)
                .addSuperinterface(BasicSubscriber::class.java)
                .addField(instanceFieldSpec).addMethod(
                    MethodSpec.methodBuilder("getInstance")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC).addJavadoc("单例")
                        .returns(mClassTypeName)
                        .beginControlFlow("if(null == \$N) ", instanceFieldSpec)
                        .beginControlFlow("synchronized (\$T.class) ", mClassTypeName)
                        .beginControlFlow("if (null == \$N)", instanceFieldSpec)
                        .addStatement("\$N = new \$T()", instanceFieldSpec, mClassTypeName)
                        .endControlFlow().endControlFlow().endControlFlow()
                        .addStatement("return \$N", instanceFieldSpec).build()
                ).addField(
                    FieldSpec.builder(
                        ClassName.get(qualifiedName, className),
                        "sub", Modifier.PRIVATE
                    ).apply {
                        if (className.endsWith("Subscriber")) {
                            initializer("new $className()")
                        }
                    }.build()
                ).apply {
                    //subscribers全局对象
                    FieldSpec.builder(
                        ParameterizedTypeName.get(ArrayList::class.java, String::class.java),
                        "subscribers",
                        Modifier.PRIVATE
                    ).initializer("new \$T<\$T>()", ArrayList::class.java, String::class.java)
                        .build().let {
                            //添加subscribers全局对象
                            addField(it)
                            //添加构造
                            addMethod(
                                MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
                                    .addJavadoc("构造函数").apply {
                                        //订阅value
                                        methodParameterTypeMap[className]?.forEach { (_, map) ->
                                            map.forEach { (subscribeId, _) ->
                                                addStatement("\$N.add(\$S)", it, subscribeId)
                                            }
                                        }
                                    }.build()
                            )
                            addMethod(
                                MethodSpec.methodBuilder("getSubscribers")
                                    .addAnnotation(Override::class.java)
                                    .addModifiers(Modifier.PUBLIC)
                                    .returns(ParameterizedTypeName.get(ArrayList::class.java, String::class.java))
                                    .addStatement("return \$N", it).build()
                            )
                        }
                    addMethod(
                        MethodSpec.methodBuilder("dispatch")
                            .addAnnotation(Override::class.java)
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(String::class.java, "id")
                            .addParameter(String::class.java, "json")
                            .apply {
                                beginControlFlow("if (null != sub && subscribers.contains(id))")
                                addStatement("android.util.Log.d(\"$mClassName\", \"dispatch -> id:\" + id +\", $className:\" + sub)")
                                beginControlFlow("switch (id)")
                                methodParameterTypeMap[className]?.forEach { (methodName, map) ->
                                    map.forEach { (subscribeId, typeMirrors) ->
                                        addCode("case \"$subscribeId\":\n")
                                        if (typeMirrors.isEmpty()) {
                                            addCode("sub.${methodName}();\n")
                                        } else {
                                            val mName = ClassName.get(typeMirrors[0]).toString()
                                            addCode("try {\n")
                                            addCode("  sub.${methodName}(($mName) com.cn.core.utils.gson.GsonUtil.INSTANCE.fromJson(json, Class.forName(\"${mName}\")));\n")
                                            addCode("")
                                            addCode("} catch (ClassNotFoundException e) {\n")
                                            addCode("  throw new RuntimeException(e);\n")
                                            addCode("}\n")
                                        }
                                        addStatement("break")
                                    }
                                }
                                endControlFlow()
                                endControlFlow()
                            }
                            .build()
                    )
                    if (!className.endsWith("Subscriber")) {
                        addMethod(
                            MethodSpec.methodBuilder("registerSubscriber")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .addParameter(ClassName.get(qualifiedName, className), "m$className")
                                .addStatement("$mClassName.getInstance().sub = m$className")
                                .addStatement("android.util.Log.d(\"$mClassName\", \"registerSubscriber:\" + $mClassName.getInstance().sub)")
                                .build()
                        )
                        addMethod(
                            MethodSpec.methodBuilder("unRegisterSubscriber")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .addStatement("$mClassName.getInstance().sub = null")
                                .addStatement("android.util.Log.d(\"$mClassName\", \"unRegisterSubscriber:\")")
                                .build()
                        )
                    }
                    methodParameterTypeMap[className]?.forEach { (methodName, map) ->
                        map.forEach { (_, typeMirrors) ->
                            MethodSpec.methodBuilder(methodName)
                                .addModifiers(Modifier.PUBLIC)
                                .apply { typeMirrors.forEach {
                                    addParameter(ParameterizedTypeName.get(it), "value")
                                } }.build()
                        }
                    }
                    /*classMethodAnnotationMap[className]?.forEach { (_, methodName) ->
                        addMethod(
                            MethodSpec.methodBuilder(methodName)
                                .addModifiers(Modifier.PUBLIC)
                                .apply { methodParameterTypeMap[className]?.get(methodName)?.forEach {
                                    addParameter(ParameterizedTypeName.get(it), "value")
                                } }.build()
                        )
                    }*/
                }.build()?.let { generate(qualifiedName, it) }
        }
    }
}