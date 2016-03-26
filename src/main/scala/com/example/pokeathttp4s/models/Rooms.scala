package com.example.pokeathttp4s.models

import org.http4s.{EntityEncoder, EntityDecoder}
import org.http4s.argonaut._
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


case class RoomInput(name: String)
object RoomInput {
  implicit val RoomInputCodecJson: CodecJson[RoomInput] =
    casecodec1(RoomInput.apply, RoomInput.unapply)("name")
  // entity encoder/decoder are for http4s' decode
  implicit val RoomInputEntityDecoder: EntityDecoder[RoomInput] = jsonOf[RoomInput]
  implicit val RoomInputEntityEncoder: EntityEncoder[RoomInput] = jsonEncoderOf[RoomInput]
}


class Rooms(tag: Tag) extends Table[Room](tag, "ROOMS") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
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

  def create(r: RoomInput): Future[Room] = {
    // query to use AutoInc on id
    val q = rooms returning rooms.map(_.id) into ((r, id) => r.copy(id = id))
    val room = Room(0, r.name, DateTime.now())

    db.run(q += room)
  }

}