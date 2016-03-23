package com.example.pokeathttp4s

package object models {
  import slick.driver.PostgresDriver.api._
  val db = Database.forConfig("database")
}
