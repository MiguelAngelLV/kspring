package org.malv.kspring.spring2mvc

import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.*




class DateStringConverter :  Converter.Factory() {

    override fun stringConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<*, String>? {
        if (type === Date::class.java)
            return DateQueryConverter

        return null
    }


}


object DateQueryConverter : Converter<Date, String> {
    override fun convert(date: Date): String {
        return "${date.time}"
    }

}