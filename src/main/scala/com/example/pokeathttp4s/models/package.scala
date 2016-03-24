package com.example.pokeathttp4s

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import argonaut._, Argonaut._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

package object models {
  val db = Database.forConfig("database")

  implicit def dateTime =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts.getTime)
    )

  val dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")

  implicit def DateTimeToJson: CodecJson[DateTime] =
    CodecJson(
      (d: DateTime) => jString(dtf.print(d)),
      d => for {
        dt <- d.as[String]
      } yield DateTime.parse(dt)
    )
}
