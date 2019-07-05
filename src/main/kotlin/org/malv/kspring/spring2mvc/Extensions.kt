package org.malv.kspring.spring2mvc

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.springframework.http.HttpStatus
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.nio.file.Files

fun File.toPart(field: String): MultipartBody.Part {

    val type = MediaType.parse(Files.probeContentType(toPath()))
    val requestFile = RequestBody.create(type, this)

    return MultipartBody.Part.createFormData(field, name, requestFile)

}


fun <T : Any> Response<T>.body(): T {
    return body() ?: throw Exception(toString())
}

fun <T : Any> Response<T>.data(): T {
    return body() ?: throw Exception(toString())
}

fun <T : Any> Response<Page<T>>.list(): List<T> {
    return data().content
}

fun <T> Response<T>.assertNotFound() {
    if (code() != HttpStatus.NOT_FOUND.value())
        throw Exception("Expected not found, get ${code()}")
}

fun <T> Response<T>.assertForbiden() {
    if (code() != HttpStatus.FORBIDDEN.value())
        throw Exception("Expected forbidden, get ${code()}")
}

fun <T> Response<T>.assertUnauthorized() {
    if (code() != HttpStatus.UNAUTHORIZED.value())
        throw Exception("Expected unauthorized, get ${code()}")
}


fun <T> Response<T>.assertSuccessful() {
    if (code() !in 200..299)
        throw Exception("Expected successful, ${code()}")
}




fun <T : Any> Call<T>.errors(): Map<String, String> {
    val response = this.execute()
    val errors = response.errorBody() ?: throw Exception("No expected error")

    val mapper = ObjectMapper()


    val type = object : TypeReference<HashMap<String, String>>() {}

    val message: Map<String, String> =  mapper.readValue(errors.string(), type)

    return mapper.readValue(message["message"], type)


}
