package org.malv.kspring.spring2mvc

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders


open class ApiBase(val mockMvc: MockMvc, val mapper: ObjectMapper) {




    fun _get(url: String, vararg arguments: Any): MockHttpServletRequestBuilder {

        return MockMvcRequestBuilders.get(url, *arguments)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(session)
    }


    fun _post(url: String, body: Any?, vararg arguments: Any): MockHttpServletRequestBuilder {

        val content = mapper.writeValueAsString(body)

        return MockMvcRequestBuilders.post(url, *arguments)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .session(session)
    }


    fun _put(url: String, body: Any, vararg arguments: Any): MockHttpServletRequestBuilder {

        val content = mapper.writeValueAsString(body)

        return MockMvcRequestBuilders.put(url, *arguments)
                .content(content)
                .accept(MediaType.APPLICATION_JSON)
                .session(session)
    }


    fun _delete(url: String, vararg arguments: Any): MockHttpServletRequestBuilder {

        return MockMvcRequestBuilders.delete(url, *arguments)
                .accept(MediaType.APPLICATION_JSON)
                .session(session)
    }


    fun _file(url: String, file: MockMultipartFile, vararg arguments: Any): MockHttpServletRequestBuilder {
        return MockMvcRequestBuilders.fileUpload(url, *arguments)
                .file(file)
                .accept(MediaType.APPLICATION_JSON)
                .session(session)
    }


    companion object {
        val session = MockHttpSession()
    }







}
