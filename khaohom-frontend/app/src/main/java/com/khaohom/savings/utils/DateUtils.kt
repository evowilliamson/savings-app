package com.khaohom.savings.utils

import java.time.LocalDate

object DateUtils {
    fun parseDate(dateString: String): LocalDate {
        val normalized = dateString.substringBefore('T')
        return LocalDate.parse(normalized)
    }
}
