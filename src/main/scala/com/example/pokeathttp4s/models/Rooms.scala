package com.example.pokeathttp4s.models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import argonaut._, Argonaut._
import scala.concurrent.Future


case class Room(id: Long, name: String, created: DateTime)

// JSON converter so we can use the case class directly in the API
object Room {
  implicit def RoomCodecJson: CodecJson[Room] =
    casecodec3(Room.apply, Room.unapply)("id", "name", "created")
}



class Rooms(tag: Tag) extends Table[Room](tag, "ROOMS") {
  def id = column[Long]("id", O.PrimaryKey)
  def name = column[String]("name")
  def created = column[DateTime]("created")

  def * = (id, name, created) <> ((Room.apply _).tupled, Room.unapply)


}

object RoomsDAO {
  val rooms = TableQuery[Rooms]

  def listAll(): Future[Seq[Room]] = {
    db.run(rooms.result)
  }

  def getById(roomId: Long): Future[Option[Room]] = {
    db.run(rooms.filter(_.id === roomId).result.headOption)
  }

}