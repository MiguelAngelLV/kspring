package org.malv.kspring.spring2mvc


import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MvcResult

class ApiResponse <T>  (val result: MvcResult, val type: TypeReference<T>, val mapper: ObjectMapper) {



    val status: Int
        get() = result.response.status




    val body: T
        get() = mapper.readValue(result.response.contentAsString, type)

    val bytes: ByteArray
        get() = result.response.contentAsByteArray


    fun assertNotFound() {
        if (status != HttpStatus.NOT_FOUND.value())
            throw Exception("Expected not found")

    }

    fun assertForbiden() {
        if (status != HttpStatus.FORBIDDEN.value())
            throw Exception("Expected forbidden")
    }

    fun assertUnauthorized() {
        if (status != HttpStatus.UNAUTHORIZED.value())
            throw Exception("Expected unauthorized")
    }


    fun errors(): Map<String, String> {

        val errors = result.resolvedException?.message ?: throw Exception("No expected error")

        val type = HashMap<String, String>()::class.java

        return mapper.readValue(errors, type)



    }



}
