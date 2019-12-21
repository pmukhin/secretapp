package com.newmotion.tvi

import java.time.format.DateTimeFormatter

object formats {

  val zonedDateTimeFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
}
