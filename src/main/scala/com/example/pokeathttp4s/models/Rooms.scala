package com.example.pokeathttp4s.models

import slick.driver.PostgresDriver.api._
import argonaut._, Argonaut._
import scala.concurrent.Future


case class Room(id: Long, name: String)

// JSON converter so we can use the case class directly in the API
object Room {
  implicit def RoomCodecJson: CodecJson[Room] =
    casecodec2(Room.apply, Room.unapply)("id", "name")
}


class Rooms(tag: Tag) extends Table[Room](tag, "ROOMS") {
  def id = column[Long]("id", O.PrimaryKey)
  def name = column[String]("name")

  def * = (id, name) <> ((Room.apply _).tupled, Room.unapply)
}

object RoomsDAO {
  val rooms = TableQuery[Rooms]

  def listAll(): Future[Seq[Room]] = {
    db.run(rooms.result)
  }

}