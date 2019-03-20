package org.malv.kspring.spring2mvc


import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.malv.kspring.KSpring.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import org.malv.kspring.spring2swagger.REST
import org.malv.kspring.spring2swagger.hasAnnotation
import org.malv.kspring.spring2swagger.javaToKotlinType
import org.springframework.data.domain.Pageable
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileWriter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement


class Spring2Mvc(val element: Element, val processingEnv: ProcessingEnvironment) {

    private val api: TypeSpec.Builder
    private val route: String

    init {

        val name = "Api${element.simpleName?.removeSuffix("Controller")}"

        api = TypeSpec.classBuilder(name)
                .superclass(ApiBase::class)
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter("mockMvc", MockMvc::class.java)
                                .addParameter("mapper", ObjectMapper::class.java)
                                .build())
                .addSuperclassConstructorParameter("mockMvc")
                .addSuperclassConstructorParameter("mapper")



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
        val getMapping = method.getAnnotation(DeleteMapping::class.java)
        val endPoint = "$route${getMapping.value.first()}"

        val spec = FunSpec.builder(method.simpleName.toString())


        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }
        val parameters = method.parameters.filter { it.hasAnnotation(RequestParam::class.java) }

        urlParams.forEach {
            spec.addParameter("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
        }


        spec.addStatement("""val call = _delete("$endPoint"${urlParams.joinToString(separator = "") { ", ${it.simpleName}" }})""")


        parameters.forEach {
            val name = "${it.simpleName}"
            val path = it.getAnnotation(RequestParam::class.java)?.value ?: name
            spec.addParameter(ParameterSpec.builder(name, it.asType().asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())


            spec.addStatement("if ($name is Iterable)")
            spec.addStatement("""\tcall.param("$path", "$name.joinToString(","))""")
            spec.addStatement("else")
            spec.addStatement("""\tcall.param("$path", "$name")""")

        }

        val response = ApiResponse::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())


        spec.returns(returnType)
                .addStatement("val perform = mockMvc.perform(call)")
                .addStatement("val result = perform.andReturn()")
                .addStatement("return %T(result, jacksonTypeRef(), mapper)", response)

        api.addFunction(spec.build())

    }

    fun createGet(method: ExecutableElement) {

        val getMapping = method.getAnnotation(GetMapping::class.java)
        val endPoint = "$route${getMapping.value.first()}"

        val spec = FunSpec.builder(method.simpleName.toString())


        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }
        val parameters = method.parameters.filter { it.hasAnnotation(RequestParam::class.java) }

        urlParams.forEach {
            spec.addParameter("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
        }





        spec.addStatement("""val call = _get("$endPoint"${urlParams.joinToString(separator = "") { ", ${it.simpleName}" }})""")

        if (method.parameters.any { it.asType().asTypeName().javaToKotlinType() == Pageable::class.asTypeName().javaToKotlinType() }) {
            spec.addParameter(ParameterSpec.builder("page", Int::class.asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())
            spec.addParameter(ParameterSpec.builder("size", Int::class.asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())
            spec.addParameter(ParameterSpec.builder("sort", String::class.asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())

            spec.addStatement("""page?.let { call.param("page", "${"$"}it") }""")
            spec.addStatement("""size?.let { call.param("size", "${"$"}it") }""")
            spec.addStatement("""sort?.let { call.param("sort", it) }""")

        }

        parameters.forEach {
            val name = "${it.simpleName}"
            val path = it.getAnnotation(RequestParam::class.java)?.value ?: name
            spec.addParameter(ParameterSpec.builder(name, it.asType().asTypeName().javaToKotlinType().asNullable()).defaultValue("null").build())

            spec.addStatement("if ($name is Iterable)")
            spec.addStatement("""\tcall.param("$path", "$name.joinToString(","))""")
            spec.addStatement("else")
            spec.addStatement("""\tcall.param("$path", "$name")""")
        }


        val response = ApiResponse::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())




        spec.returns(returnType)
                .addStatement("val perform = mockMvc.perform(call)")
                .addStatement("val result = perform.andReturn()")
                .addStatement("return %T(result, jacksonTypeRef(), mapper)", response)

        api.addFunction(spec.build())


    }

    fun createPost(method: ExecutableElement) {
        val postMapping = method.getAnnotation(PostMapping::class.java) ?: return
        val endPoint = "$route${postMapping.value.first()}"

        val spec = FunSpec.builder("${method.simpleName}")

        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }



        urlParams.forEach {
            spec.addParameter("${it.simpleName}", it.asType().asTypeName().javaToKotlinType().asNonNull())
        }


        val body = method.parameters.first { it.hasAnnotation(RequestBody::class.java) }

        spec.addParameter("${body.simpleName}", body.asType().asTypeName().javaToKotlinType())


        spec.addStatement("""val call = _post("$endPoint", ${body.simpleName}${urlParams.joinToString(separator = "") { ", ${it.simpleName}" }})""")


        val response = ApiResponse::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())

        spec.returns(returnType)
                .addStatement("val perform = mockMvc.perform(call)")
                .addStatement("val result = perform.andReturn()")
                .addStatement("return %T(result, jacksonTypeRef(), mapper)", response)

        api.addFunction(spec.build())
    }


    fun createPut(method: ExecutableElement) {
        val postMapping = method.getAnnotation(PutMapping::class.java)
        val endPoint = "$route${postMapping.value.first()}"

        val spec = FunSpec.builder("${method.simpleName}")

        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }



        urlParams.forEach {
            spec.addParameter("${it.simpleName}", it.asType().asTypeName().javaToKotlinType().asNonNull())
        }


        val body = method.parameters.first { it.hasAnnotation(RequestBody::class.java) }

        spec.addParameter("${body.simpleName}", body.asType().asTypeName().javaToKotlinType())



        spec.addStatement("""val call = _put("$endPoint", ${body.simpleName}${urlParams.joinToString(separator = "")  { ", ${it.simpleName}" }})""")


        val response = ApiResponse::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())

        spec.returns(returnType)
                .addStatement("val perform = mockMvc.perform(call)")
                .addStatement("val result = perform.andReturn()")
                .addStatement("return %T(result, jacksonTypeRef(), mapper)", response)

        api.addFunction(spec.build())
    }


    fun createFile(method: ExecutableElement) {

        val postMapping = method.getAnnotation(PostMapping::class.java) ?: return
        val endPoint = "$route${postMapping.value.first()}"

        val spec = FunSpec.builder("${method.simpleName}")

        val urlParams = method.parameters.filter { it.hasAnnotation(PathVariable::class.java) }



        urlParams.forEach {
            spec.addParameter("${it.simpleName}", it.asType().asTypeName().javaToKotlinType().asNonNull())
        }


        val file = method.parameters.find { it.hasAnnotation(RequestPart::class.java) }?.simpleName?.toString() ?: "file"


        spec.addParameter(file, MockMultipartFile::class.asTypeName().javaToKotlinType())

        spec.addStatement("""val call = _file("$endPoint", $file${urlParams.joinToString(separator = "") { ", ${it.simpleName}" }})""")


        val response = ApiResponse::class.asClassName()
        val returnType = response.parameterizedBy(method.returnType.asTypeName().javaToKotlinType())

        spec.returns(returnType)
                .addStatement("val perform = mockMvc.perform(call)")
                .addStatement("val result = perform.andReturn()")
                .addStatement("return %T(result, jacksonTypeRef(), mapper)", response)

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
