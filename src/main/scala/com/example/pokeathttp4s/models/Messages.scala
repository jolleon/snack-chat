package com.example.pokeathttp4s.models

import org.http4s.{EntityEncoder, EntityDecoder}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import argonaut._, Argonaut._
import org.http4s.argonaut._

import scala.concurrent.Future


case class Message(id: Long, roomId: Long, author: String, content: String, created: DateTime)

// JSON converter so we can use the case class directly in the API
object Message {
  implicit def MessageCodecJson: CodecJson[Message] =
    casecodec5(Message.apply, Message.unapply)("id", "roomId", "author", "content", "created")
}

case class MessageInput(author: String, content: String)
object MessageInput {
  implicit val MessageInputCodecJson: CodecJson[MessageInput] =
    casecodec2(MessageInput.apply, MessageInput.unapply)("author", "content")
  // entity encoder/decoder are for http4s' decode
  implicit val MessageInputEntityDecoder: EntityDecoder[MessageInput] = jsonOf[MessageInput]
  implicit val MessageInputEntityEncoder: EntityEncoder[MessageInput] = jsonEncoderOf[MessageInput]
}


class Messages(tag: Tag) extends Table[Message](tag, "MESSAGES") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def roomId = column[Long]("roomId")
  def author = column[String]("author")
  def content = column[String]("content")
  def created = column[DateTime]("created")

  def * = (id, roomId, author, content, created) <> ((Message.apply _).tupled, Message.unapply)

  def room = foreignKey("ROOM_FK", roomId, RoomsDAO.rooms)(_.id)
}

object MessagesDAO {
  val messages = TableQuery[Messages]

  def forRoom(roomId: Long): Future[Seq[Message]] = {
    val q = messages.filter(_.roomId === roomId)
    db.run(q.result)
  }

  def postToRoom(roomId: Long, message: MessageInput): Future[Message] = {
    // query to use AutoInc on id
    val q = messages returning messages.map(_.id) into ((m, id) => m.copy(id = id))
    val mess = Message(0, roomId, message.author, message.content, DateTime.now())

    db.run(q += mess)
  }
}