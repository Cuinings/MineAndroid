package com.cn.core.remote.msg.subscriber.processor

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.cn.core.remote.msg.subscriber.processor.FilerExt.generate
import com.cn.core.remote.msg.subscriber.processor.SubscriberUtil.applicationId
import com.cn.core.remote.msg.subscriber.processor.SubscriberUtil.classQualifiedNameMap
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

/**
 * @Author: CuiNing
 * @Time: 2024/12/25 14:23
 * @Description:
 */
object SubscriberCallbackGenerate {

    fun Filer.generateSubscriberCallback(println: (String) -> Unit) {
        TypeSpec.classBuilder("SubscriberCallback").addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get("com.cn.core.remote.msg.router.client.callback", "MsgCallback"))
            .addField(
                FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get("java.util", "HashMap"),
                        ClassName.get("java.lang", "String"),
                        ClassName.get("com.cn.library.remote.msg.subscriber.annotation", "BasicSubscriber")),
                    "processes",
                    Modifier.PRIVATE,
                ).initializer("new \$T<>()", HashMap::class.java).build()
            ).addMethod(
                MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addJavadoc("构造函数")
                    .addStatement("com.cn.core.remote.msg.router.client.MsgRouter.INSTANCE.bind(this)")
                    .apply {
                        classQualifiedNameMap.forEach { (className, qualifiedName) ->
                            ClassName.get(qualifiedName, "${className}Sub").let {
                                addStatement("processes.put(\$S, $qualifiedName.${className}Sub.getInstance())", "${className}Sub")
                            }
                        }
                    }
                    .build()
            ).addMethod(
                MethodSpec.methodBuilder("onRcvMsg").addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC).addParameter(
                        ParameterSpec.builder(ClassName.get("com.cn.core.remote.msg.router.client.bean", "Msg"), "msg",).build()
                    ).addParameter(
                        String::class.java, "target"
                    )
                    .addStatement("android.util.Log.d(this.getClass().getSimpleName(), \"onRcvMsg:\" + msg.getContent().getCode())")
                    .addCode("for (HashMap.Entry<String, BasicSubscriber> entry: processes.entrySet()) {\n" +
                            " entry.getValue().dispatch(msg.getContent().getCode(), msg.getContent().getBody());\n" +
                            "}\n")
                    .build()
            ).apply {
                println("create onSubScribe method")
                addMethod(MethodSpec.methodBuilder("onSubScribe")
                    .addAnnotation(Override::class.java).addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(ArrayList::class.java, String::class.java)).addStatement(
                        "\$T mSubscribes = new \$T()", ParameterizedTypeName.get(ArrayList::class.java, String::class.java), ParameterizedTypeName.get(ArrayList::class.java, String::class.java)
                    ).apply {
                        beginControlFlow("for (HashMap.Entry<String, BasicSubscriber> entry: processes.entrySet())")
                        addStatement("entry.getValue().getSubscribers().forEach( it -> { if (!mSubscribes.contains(it)) mSubscribes.add(it); })")
                        endControlFlow()
                    }.addStatement("return mSubscribes").build())
            }.build().let { generate(applicationId, it) }
    }
}