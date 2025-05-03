package com.cn.library.remote.msg.subscriber.processor

import com.cn.library.remote.msg.subscriber.annotation.Subscriber
import com.google.auto.service.AutoService
import com.cn.library.remote.msg.subscriber.processor.SubscriberCallbackGenerate.generateSubscriberCallback
import com.cn.library.remote.msg.subscriber.processor.SubscriberGenerate.generateSubscriber
import com.cn.library.remote.msg.subscriber.processor.SubscriberUtil.applicationId
import com.cn.library.remote.msg.subscriber.processor.SubscriberUtil.classQualifiedNameMap
import com.cn.library.remote.msg.subscriber.processor.SubscriberUtil.methodParameterTypeMap
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedAnnotationTypes("com.cn.library.remote.msg.subscribe.annotation.Subscriber")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
class SubscriberProcessor: AbstractProcessor() {

    private lateinit var elements: Elements
    private lateinit var types: Types
    private lateinit var messager: Messager
    private lateinit var filer: Filer

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        processingEnv.let {
            elements = it.elementUtils
            messager = it.messager
            filer = it.filer
            applicationId = it.options["appId"]?:""
        }
    }

    override fun getSupportedAnnotationTypes(): Set<String> = LinkedHashSet<String>().apply { add(Subscriber::class.java.canonicalName) }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.RELEASE_17

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isEmpty()) return false
        roundEnv.getElementsAnnotatedWith(Subscriber::class.java)?.let { elementSet ->
            elementSet.takeIf { it.isNotEmpty() }?.forEach { element ->
                //路径
                val mQualifiedName = elements.getPackageOf(element).qualifiedName.toString()
                //类名
                val mClassName = element.enclosingElement.simpleName.toString()
                classQualifiedNameMap[mClassName] = mQualifiedName
                var subscribeId = element.getAnnotation(Subscriber::class.java).subscribeId//注解value
                val executableElement = element as ExecutableElement
                val methodName = executableElement.simpleName.toString()
                //如果注解中没有subscribeId使用方法名代替subscribeId
                if (subscribeId.isBlank()) subscribeId = methodName

                if (null == methodParameterTypeMap[mClassName])
                    methodParameterTypeMap[mClassName] = HashMap()
                if (null == methodParameterTypeMap[mClassName]?.get(methodName))
                    methodParameterTypeMap[mClassName]?.put(methodName, HashMap())
                if (null == methodParameterTypeMap[mClassName]?.get(methodName)?.get(subscribeId))
                    methodParameterTypeMap[mClassName]?.get(methodName)?.put(subscribeId, ArrayList())

//                if (null == classMethodAnnotationMap[mClassName])
//                    classMethodAnnotationMap[mClassName] = HashMap()
//                classMethodAnnotationMap[mClassName]?.takeIf { !it.containsKey(subscribeId) }?.put(subscribeId, methodName)

                messager.println("$mClassName, $subscribeId, $methodName")
                //处理方法参数
                executableElement.parameters.forEach {
                    methodParameterTypeMap[mClassName]?.get(methodName)?.get(subscribeId)?.add(it.asType())
                }
            }

            println("createFiles:start")
            println("appId -> $applicationId")
            messager.println("generateSubscriber ")
            filer.generateSubscriber { messager.println("generateSubscriber $it") }
            messager.println("generateSubscriberCallback ")
            filer.generateSubscriberCallback { messager.println("generateSubscriberCallback $it") }
            println("createFiles:end")
        }
        return true
    }


    private fun Messager.println(msg: String) {
        this.printMessage(Diagnostic.Kind.WARNING, "Subscribe -> $msg")
    }
}