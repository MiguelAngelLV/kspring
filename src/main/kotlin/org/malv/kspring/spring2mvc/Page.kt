package org.malv.kspring.spring2mvc

class Page<T>  {

    var size: Int = 0
    var totalPages: Int = 0
    var totalElements: Long = 0L
    var content: List<T> = emptyList()


}
