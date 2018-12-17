package org.malv.kspring

import com.google.auto.service.AutoService
import org.malv.kspring.spring2mvc.Spring2Mvc
import org.malv.kspring.spring2swagger.REST
import org.malv.kspring.spring2swagger.SwaggerProcessor
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*

@AutoService(Processor::class)
class KSpring : AbstractProcessor() {


    lateinit var swagger: SwaggerProcessor


    override fun init(p: ProcessingEnvironment?) {
        super.init(p)

        swagger = SwaggerProcessor(processingEnv)


    }


    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(REST::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(REST::class.java)
                .forEach {
                    swagger.generateClass(it)
                    Spring2Mvc(it, processingEnv)
                }
        return true
    }





    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}