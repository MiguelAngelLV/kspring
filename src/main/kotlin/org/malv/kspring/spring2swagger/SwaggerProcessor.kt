package org.malv.kspring.spring2swagger

import com.squareup.kotlinpoet.*
import io.swagger.annotations.*
import org.malv.kspring.KSpring.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import org.springframework.web.bind.annotation.*
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement


class SwaggerProcessor(val processingEnv: ProcessingEnvironment) {

    lateinit var actualClass: TypeElement

    fun generateClass(element: Element) {


        actualClass = element as TypeElement

        val fileName = "${element.simpleName}Swagger"
        val controller = TypeSpec.classBuilder(fileName)
        controller.superclass(element.asType().asTypeName())

        val endPoint = element.getAnnotation(REST::class.java).name

        controller.addAnnotation(RestController::class.java)
        controller.addAnnotation(AnnotationSpec
                .builder(RequestMapping::class.java).addMember("%S", endPoint)
                .build())


        val description = getDocumentLine("Description", element)
        val summary = getDocumentLine("Summary", element)

        controller.addAnnotation(AnnotationSpec.builder(Api::class.java)
                .addMember("value = %S", endPoint)
                .addMember("tags = [%S]", summary)
                .addMember("description = %S", description)
                .build()
        )



        element.enclosedElements
                .filterIsInstance(ExecutableElement::class.java)
                .filter {
                    it.hasAnnotation(GetMapping::class.java)
                            || it.hasAnnotation(PostMapping::class.java)
                            || it.hasAnnotation(DeleteMapping::class.java)
                            || it.hasAnnotation(PutMapping::class.java)
                }
                .forEach {
                    controller.addFunction(generateMethods(it))
                }


        val file = FileSpec.builder("${processingEnv.elementUtils.getPackageOf(element)}", fileName)
                .addType(controller.build())
                .build()

        val dir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(dir, "$fileName.kt"))
    }


    fun generateMethods(element: ExecutableElement): FunSpec {

        val function = FunSpec.builder(element.simpleName.toString())

        element.parameters.forEach {
            val param = ParameterSpec.builder("${it.simpleName}", it.asType().asTypeName().javaToKotlinType())
            it.annotationMirrors.forEach {
                param.addAnnotation(AnnotationSpec.get(it))
            }



            function.addParameter(param.build())
        }


        val summary = getDocumentLine("Summary", element)
        val description = getDocumentLine("Description", element)


        function.addAnnotation(
                AnnotationSpec.builder(ApiOperation::class.java)
                        .addMember("value = %S", summary)
                        .addMember("notes = %S", description)
                        .build())


        function.addAnnotation(generateResponses(element))


        generateImplicitParams(element, function)

        function.modifiers.add(KModifier.OVERRIDE)
        function.addStatement("return super.${element.simpleName}(${element.parameters.joinToString { p -> p.simpleName }})")

        function.returns(element.returnType.asTypeName().javaToKotlinType())



        return function.build()

    }


    fun generateImplicitParams(element: ExecutableElement, function: FunSpec.Builder) {

        val params = getDocumentLines("Param", element)

        if (params.isEmpty()) return


        val annotation = AnnotationSpec.builder(ApiImplicitParams::class.java)

        val implicitParams = params.map {

            val name = it.substringBefore(" ")
            val type = it.substringBefore(" ")

            AnnotationSpec.builder(ApiImplicitParam::class.java)
                    .addMember("type = %S", type)
                    .addMember("name = %S", name)
                    .addMember("requited = true")
                    .build()
        }


        annotation.addMember("${params.joinToString { "%L" }}", *implicitParams.toTypedArray())

        function.addAnnotation(annotation.build())


    }


    fun generateResponses(element: Element): AnnotationSpec {

        val comments = getDocumentLines("Response", element)
        val response = AnnotationSpec.builder(ApiResponses::class.java)

        val responses = comments.map {
            val code = it.substringBefore(" ")
            val message = it.substringAfter(" ")
            AnnotationSpec.builder(ApiResponse::class.java)
                    .addMember("code = $code")
                    .addMember("message = %S", message)
                    .build()
        }


        when {
            responses.isEmpty() -> throw Exception("The element $element ( $actualClass ) don't have response documentation")
            else -> response.addMember("${responses.joinToString { "%L" }}", *responses.toTypedArray())
        }

        return response.build()
    }


    fun getDocument(element: Element): String {
        return processingEnv.elementUtils.getDocComment(element) ?: "No document"
    }


    fun getDocumentLine(type: String, element: Element): String {
        return getDocumentLines(type, element).firstOrNull()
                ?: throw Exception("The element $element  ( $actualClass )  don't have $type documentation")

    }

    fun getDocumentLineOptional(type: String, element: Element): String? {
        return getDocumentLines(type, element).firstOrNull()

    }

    fun getDocumentLines(type: String, element: Element): List<String> {
        val document = getDocument(element).split("\n").map { it.replace("*", "").trim() }
        return document.filter { it.startsWith("@$type") }.map { it.replace("@$type ", "") }
    }

}