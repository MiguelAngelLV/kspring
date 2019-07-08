package org.malv.kspring.spring2mvc


import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.annotations.Nullable
import org.malv.kspring.KSpring.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import org.malv.kspring.spring2swagger.REST
import org.malv.kspring.spring2swagger.hasAnnotation
import org.malv.kspring.spring2swagger.javaToKotlinType
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*
import retrofit2.Call
import retrofit2.http.*
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement


class Spring2Mvc(val element: Element, val processingEnv: ProcessingEnvironment) {

    private val api: TypeSpec.Builder
    private val route: String

    init {

        val name = "Api${element.simpleName?.removeSuffix("Controller")}"

        api = TypeSpec.interfaceBuilder(name)

        route = element.getAnnotation(REST::class.java).name

        element.enclosedElements
                .filterIsInstance(ExecutableElement::class.java)
                .filter {
                    it.hasAnnotation(GetMapping::class.java)
                            || it.hasAnnotation(PostMapping::class.java)
                            || it.hasAnnotation(DeleteMapping::class.java)
                            || it.hasAnnotation(PutMapping::class.java)
                }
                .forEach {
                    generateMethods(it)
                }



        createClass()

    }


    fun generateMethods(m: ExecutableElement) {

        when {
            m.hasAnnotation(PostMapping::class.java) && m.parameters.any { it.hasAnnotation(RequestPart::class.java) } -> createFile(m)
            m.hasAnnotation(GetMapping::class.java) -> createGet(m)
            m.hasAnnotation(PostMapping::class.java) -> createPost(m)
            m.hasAnnotation(DeleteMapping::class.java) -> createDelete(m)
            m.hasAnnotation(PutMapping::class.java) -> createPut(m)
        }


    }


    fun createDelete(method: ExecutableElement) {
        val mapping = method.getAnnotation(DeleteMapping::class.java)
        val endPoint = "$route${mapping.value.first()}"

        val spec = FunSpec.builder(method.simpleName.toString())
                .addModifiers(KModifier.ABSTRACT)


        spec.addAnnotation(AnnotationSpec.builder(DELETE::class.java)
                .addMember("value = %S", endPoint)
                .build()
        )


        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }
        val parameters = method.parameters.filter { it.hasAnnotation(RequestParam::class.java) }

        urlParams.forEach {

            spec.addParameter(
                    ParameterSpec.builder("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
                            .addAnnotation(AnnotationSpec.builder(Path::class.java).addMember("value = %S", it.simpleName).build())
                            .build()
            )
        }


        parameters.forEach {
            val name = "${it.simpleName}"
            val path = it.getAnnotation(RequestParam::class.java)?.value ?: name
            spec.addParameter(
                    ParameterSpec.builder(name, it.asType().asTypeName().javaToKotlinType().asNullable()).defaultValue("null")
                            .addAnnotation(AnnotationSpec.builder(Query::class.java).addMember("value = %S", path).build())
                            .build()

            )
        }

        val response = Call::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())


        spec.returns(returnType)


        api.addFunction(spec.build())

    }

    fun createGet(method: ExecutableElement) {

        val mapping = method.getAnnotation(GetMapping::class.java)
        val endPoint = "$route${mapping.value.first()}"

        val spec = FunSpec.builder(method.simpleName.toString())
                .addModifiers(KModifier.ABSTRACT)


        spec.addAnnotation(AnnotationSpec.builder(GET::class.java)
                .addMember("value = %S", endPoint)
                .build()
        )


        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }
        val parameters = method.parameters.filter { it.hasAnnotation(RequestParam::class.java) }

        urlParams.forEach {

            spec.addParameter(
                    ParameterSpec.builder("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
                            .addAnnotation(AnnotationSpec.builder(Path::class.java).addMember("value = %S", it.simpleName).build())
                            .build()
            )
        }


        parameters.forEach {
            val name = "${it.simpleName}"
            val path = it.getAnnotation(RequestParam::class.java)?.value ?: name
            spec.addParameter(
                    ParameterSpec.builder(name, it.asType().asTypeName().javaToKotlinType().asNullable()).defaultValue("null")
                            .addAnnotation(AnnotationSpec.builder(Query::class.java).addMember("value = %S", path).build())
                            .build()

            )
        }


        if (method.parameters.any { it.asType().asTypeName().javaToKotlinType() == Pageable::class.asTypeName().javaToKotlinType() }) {
            spec.addParameter(ParameterSpec.builder("page", Int::class.asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())
            spec.addParameter(ParameterSpec.builder("size", Int::class.asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())
            spec.addParameter(ParameterSpec.builder("sort", String::class.asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())
        }



        val response = Call::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())


        spec.returns(returnType)


        api.addFunction(spec.build())


    }

    fun createPost(method: ExecutableElement) {




        val mapping = method.getAnnotation(PostMapping::class.java)
        val endPoint = "$route${mapping.value.first()}"

        val spec = FunSpec.builder(method.simpleName.toString())
                .addModifiers(KModifier.ABSTRACT)

        spec.addAnnotation(AnnotationSpec.builder(POST::class.java)
                .addMember("value = %S", endPoint)
                .build()
        )



        val body = method.parameters.firstOrNull { it.hasAnnotation(RequestBody::class.java) } ?: throw Exception("POST method ($endPoint) don't  have Body")


        spec.addParameter(ParameterSpec.builder("${body.simpleName}",  body.asType().asTypeName().javaToKotlinType())
                .addAnnotation(Body::class.java)
                .build())


        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }
        val parameters = method.parameters.filter { it.hasAnnotation(RequestParam::class.java) }

        urlParams.forEach {

            spec.addParameter(
                    ParameterSpec.builder("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
                            .addAnnotation(AnnotationSpec.builder(Path::class.java).addMember("value = %S", it.simpleName).build())
                            .build()
            )
        }


        parameters.forEach {
            val name = "${it.simpleName}"
            val path = it.getAnnotation(RequestParam::class.java)?.value ?: name
            spec.addParameter(
                    ParameterSpec.builder(name, it.asType().asTypeName().javaToKotlinType().asNullable()).defaultValue("null")
                            .addAnnotation(AnnotationSpec.builder(Query::class.java).addMember("value = %S", path).build())
                            .build()

            )
        }

        val response = Call::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())


        spec.returns(returnType)


        api.addFunction(spec.build())

    }


    fun createPut(method: ExecutableElement) {
        val mapping = method.getAnnotation(PutMapping::class.java)
        val endPoint = "$route${mapping.value.first()}"

        val spec = FunSpec.builder(method.simpleName.toString())

        spec.addAnnotation(AnnotationSpec.builder(PUT::class.java)
                .addMember("value = %S", endPoint)
                .build()
        )



        val body = method.parameters.firstOrNull { it.hasAnnotation(RequestBody::class.java) } ?: throw Exception("POST method ($endPoint) don't  have Body")


        spec.addParameter(ParameterSpec.builder("${body.simpleName}",  body.asType().asTypeName().javaToKotlinType())
                .addAnnotation(Body::class.java)
                .build())


        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }
        val parameters = method.parameters.filter { it.hasAnnotation(RequestParam::class.java) }

        urlParams.forEach {

            spec.addParameter(
                    ParameterSpec.builder("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
                            .addAnnotation(AnnotationSpec.builder(Path::class.java).addMember("value = %S", it.simpleName).build())
                            .build()
            )
        }


        parameters.forEach {
            val name = "${it.simpleName}"
            val path = it.getAnnotation(RequestParam::class.java)?.value ?: name
            spec.addParameter(
                    ParameterSpec.builder(name, it.asType().asTypeName().javaToKotlinType().asNullable()).defaultValue("null")
                            .addAnnotation(AnnotationSpec.builder(Query::class.java).addMember("value = %S", path).build())
                            .build()

            )
        }

        val response = Call::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())


        spec.returns(returnType)


        api.addFunction(spec.build())
    }


    fun createFile(method: ExecutableElement) {

        val mapping = method.getAnnotation(PostMapping::class.java)
        val endPoint = "$route${mapping.value.first()}"

        val spec = FunSpec.builder(method.simpleName.toString())
                .addModifiers(KModifier.ABSTRACT)


        spec.addAnnotation(AnnotationSpec.builder(POST::class.java)
                .addMember("value = %S", endPoint)
                .build()
        )




        val file = method.parameters.find { it.hasAnnotation(RequestPart::class.java) }?.simpleName?.toString() ?: "file"


        spec.addParameter(file, Part::class.asTypeName().javaToKotlinType())



        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }
        val parameters = method.parameters.filter { it.hasAnnotation(RequestParam::class.java) }

        urlParams.forEach {

            spec.addParameter(
                    ParameterSpec.builder("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
                            .addAnnotation(AnnotationSpec.builder(Path::class.java).addMember("value = %S", it.simpleName).build())
                            .build()
            )
        }


        parameters.forEach {
            val name = "${it.simpleName}"
            val path = it.getAnnotation(RequestParam::class.java)?.value ?: name
            spec.addParameter(
                    ParameterSpec.builder(name, it.asType().asTypeName().javaToKotlinType().asNullable()).defaultValue("null")
                            .addAnnotation(AnnotationSpec.builder(Query::class.java).addMember("value = %S", path).build())
                            .build()

            )
        }

        val response = Call::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())


        spec.returns(returnType)


        api.addFunction(spec.build())









    }


    fun createClass() {
        val type = api.build()


        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()

        val name = type.name ?: "NoName"
        val file = FileSpec.builder(packageName, name)
                .addType(type)
                .addImport("com.fasterxml.jackson.module.kotlin", "jacksonTypeRef")
                .build()



        val dir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val writer = FileWriter(File(dir, "$name.kt"))


        var content = file.toString()
        content = content.replace("import org.springframework.data.domain.Page", "import org.malv.kspring.spring2mvc.Page")
        writer.write(content)
        writer.flush()


    }


}
