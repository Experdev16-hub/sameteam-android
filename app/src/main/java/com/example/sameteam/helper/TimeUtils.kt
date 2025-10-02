package com.example.sameteam.helper

import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

private const val MMMM_dd_PATTERN = "MMMM dd"
private const val ddMMyyyy_PATTERN = "ddMMyyyy"
private const val dd_MMM_yyyy_PATTERN = "dd-MMM-yyyy"
private const val yyyy_MM_dd_PATTERN = "yyyy-MM-dd"
private const val dd_MMM_yyyy_HH_mm_PATTERN = "dd-MMM-yyyy hh:mm a"
private const val HH_mm_ss = "HH:mm:ss"
private const val HH_mm = "HH:mm"
private const val hh_mm_a = "hh:mm a"


fun getDate(milliseconds: Long): String {
    val dateFormat = SimpleDateFormat(MMMM_dd_PATTERN, Locale.getDefault())
    return dateFormat.format(Date(milliseconds))
}

fun getDateAsHeaderId(milliseconds: Long): Long {
    val dateFormat = SimpleDateFormat(ddMMyyyy_PATTERN, Locale.getDefault())
    return java.lang.Long.parseLong(dateFormat.format(Date(milliseconds)))
}

fun asDate(localDate: LocalDate): Date {
    return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
}

fun asLocalDate(date: Date): LocalDate {
    return Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun asCalendar(localDate: LocalDate): Calendar{
    val calendar = Calendar.getInstance()
    calendar.set(localDate.year,localDate.monthValue-1,localDate.dayOfMonth)
    return calendar
}

fun getDateFromString(value: String): LocalDate {
    val format = SimpleDateFormat(dd_MMM_yyyy_PATTERN, Locale.getDefault())
    return asLocalDate(format.parse(value)!!)
}

fun getDateFromString2(value: String): LocalDate {
    val format = SimpleDateFormat(yyyy_MM_dd_PATTERN, Locale.getDefault())
    return asLocalDate(format.parse(value)!!)
}

fun localToUTC(string: String): String {

    val sdf = SimpleDateFormat(hh_mm_a, Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()

    val sdfOutPutToSend = SimpleDateFormat(HH_mm_ss, Locale.getDefault())
    sdfOutPutToSend.timeZone = TimeZone.getTimeZone("UTC")

    val date = sdf.parse(string)
    return sdfOutPutToSend.format(date)
}

fun utcToLocal(string: String): String{

    try{
        val sdf = SimpleDateFormat(HH_mm_ss, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val sdfOutPutToSend = SimpleDateFormat(hh_mm_a, Locale.getDefault())
        sdfOutPutToSend.timeZone = TimeZone.getDefault()

        val date = sdf.parse(string)

        return sdfOutPutToSend.format(date)
    }
    catch (e: Exception){
        val sdf = SimpleDateFormat(HH_mm, Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC");

        val sdfOutPutToSend = SimpleDateFormat(hh_mm_a, Locale.getDefault())
        sdfOutPutToSend.timeZone = TimeZone.getDefault()

        val date = sdf.parse(string)

        return sdfOutPutToSend.format(date)
    }


}

fun localToUTCTimestamp(dateTimeString : String): Long?{

    try{
        val sdf = SimpleDateFormat("dd-MMM-yyyy'T'hh:mm a.SSSZ", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(dateTimeString)
        if (date != null) {
            return date.time/1000
        }
        return null
    }
    catch(e: Exception){
        e.printStackTrace()
        return null
    }
}

fun utcTimestampToLocalDateTime(timestamp: String): LocalDateTime? {

    return try{
        val dt = Instant.ofEpochSecond(timestamp.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        dt
    }
    catch (e: Exception){
        e.printStackTrace()
        null
    }
}

fun getFormattedDate(localDate: LocalDate): String{
    try {
        val mDay =
            if (localDate.dayOfMonth in 1..9) "0${localDate.dayOfMonth}" else "${localDate.dayOfMonth}"
        return "$mDay-${
            localDate.month.getDisplayName(
                TextStyle.SHORT,
                Locale.ENGLISH
            )
        }-${localDate.year}"
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}

fun getFormattedTime(localTime: LocalTime): String{
    try {
        return localTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}




