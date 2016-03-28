# snack-chat
Poking at http4s + slick3 + postgres + backbone.js to build a chat app.

[Try it live!](http://snack-chat.herokuapp.com/)

## Backend

Backend is built in Scala using:
- [http4s](https://github.com/http4s/http4s) as the web framework
- [Slick 3](http://slick.typesafe.com/doc/3.1.0/index.html) for DB access
- [Postgres](http://www.postgresql.org/) as the database
- [Argonaut](http://argonaut.io/) for Json parsing (Argonaut is included in http4s)
- [Scalaz](https://github.com/scalaz/scalaz) for functional purists (used by http4s)

I hadn't used any of these before, so take my setup with a grain of salt.

Http4s has zero documentation so you're on your own to figure it out. After some time I found [this project](https://github.com/IronCoreLabs/http4s-demo) which contains great examples.

It may have been smarter to get familiar with scalaz in isolation first, as a number of idioms used in http4s and Argonaut build on it. The lack of documentation + unfamiliarity with Scalaz made these tough to pick up.

Here are the main obstacles I hit, hopefully this can help the next person setting up a similar app.

### Heroku setup

Make sure to bind to `0.0.0.0` and get the port from the environment or heroku won't find your app:
```
  val port = sys.env.getOrElse("PORT", "5000").toInt

  BlazeBuilder.bindHttp(port, "0.0.0.0")
    .mountService(ChatService.service, "/")
    .run
    .awaitShutdown()
```

Similarly read the db config from the environment. You can figure out variables of interest like so:
```
Juless-MBP:~/Dropbox/code/scala/poke-at-http4s (master) heroku run sbt console
Running sbt console on radiant-river-36013.... up, run.1806
Downloading sbt launcher for 0.13.7:
...
...
Welcome to Scala 2.11.8 (OpenJDK 64-Bit Server VM, Java 1.8.0_74-cedar14).
Type in expressions for evaluation. Or try :help.

scala> sys.env foreach { case (k, v) => if (k.contains("DATABASE")) println(s"$k: $v") }
DATABASE_URL: postgres://<redacted>
JDBC_DATABASE_PASSWORD: <redacted>
JDBC_DATABASE_URL: jdbc:postgresql://<redacted>
JDBC_DATABASE_USERNAME: <redacted>
```

### Database / Slick setup

After trying a number of combinations of options, I found one that works both locally and with Heroku Postgres.

Database creation in a [Models package object](https://github.com/jolleon/snack-chat/blob/master/src/main/scala/com/example/pokeathttp4s/models/package.scala):
```
import slick.driver.PostgresDriver.api._

...
val db = Database.forConfig("database")
```
(doesn't need to be in the package object but this makes it conveniently accessible from all models)

Database configuration in [application.conf](https://github.com/jolleon/snack-chat/blob/master/src/main/resources/application.conf) that works both locally and with Heroku. `resources` seemed like the right place for this file since it's already in the classpath.
```
database {
  dataSourceClass = "slick.jdbc.DatabaseUrlDataSource"
  properties = {
    driver = "slick.driver.PostgresDriver$"
    // set default dev db - next line overrides with env variable set by Heroku
    url = "jdbc:postgresql://localhost:5432/jules?user=jules&password="
    url = ${?JDBC_DATABASE_URL}
  }
}
```

### Making Argonaut and Slick play nice with each other

It's convenient to be able to use the same model case classes for both db access and json serialization.

For this to work, include implicit Json codecs with the case classes, e.g. in the [model file](https://github.com/jolleon/snack-chat/blob/master/src/main/scala/com/example/pokeathttp4s/models/Messages.scala):
```
import argonaut._, Argonaut._
import org.http4s.argonaut._

case class Message(id: Long, roomId: Long, author: String, content: String, created: DateTime)

// JSON converter so we can use the case class directly in the API
object Message {
  implicit def MessageCodecJson: CodecJson[Message] =
    casecodec5(Message.apply, Message.unapply)("id", "roomId", "author", "content", "created")
}
```
BTW notice the `5` in `casecodec5` - this is the number of fields in the tuple.

### Parsing Json body with http4s-argonaut

This one was a rather pleasant surprise - once I figured how to make it work.

I like to have a separate case class for model creation (which contains only the user provided fields), e.g.:
```
case class MessageInput(author: String, content: String)
```
Http4s has a convenient `decode` method on the request that will parse the request body for you:
```
    case req @ POST -> Root / "api" / "rooms" / roomId / "messages" =>
      req.decode[MessageInput] { (m: MessageInput) =>
        ...
```
To make this work though you'll need not only an implicit Json codec, but also an implicit "`EntityDecoder`":
```
case class MessageInput(author: String, content: String)

object MessageInput {
  implicit val MessageInputCodecJson: CodecJson[MessageInput] =
    casecodec2(MessageInput.apply, MessageInput.unapply)("author", "content")
  // entity encoder/decoder are for http4s' decode
  implicit val MessageInputEntityDecoder: EntityDecoder[MessageInput] = jsonOf[MessageInput]
  implicit val MessageInputEntityEncoder: EntityEncoder[MessageInput] = jsonEncoderOf[MessageInput]
}
```

### Future vs Task

In vanilla scala we're used to dealing with "Futures" to represent non blocking operations. This is for instance what Slick will return from a `db.run(q.result)`. Scalaz has a similar concept called `Task` - which I don't understand well enough to explain here.

Http4s expects responses to come back in a Task and not a Future, so if you're dealing with libraries like Slick some conversions are in order. See [Util.scala](https://github.com/jolleon/snack-chat/blob/master/src/main/scala/com/example/pokeathttp4s/Util.scala) for how to turn a Future into a Task:
```
  def futureToTask[A](f: => Future[A])(implicit ec: ExecutionContext): Task[A] = {
    Task.async { cb =>
      f.onComplete {
        case Success(a) => cb(a.right)
        case Failure(t) => cb(t.left)
      }
    }
  }
```

This can be used like so:
```
  case req @ POST -> Root / "api" / "rooms" / roomId / "messages" =>
    req.decode[MessageInput] { (m: MessageInput) =>
      Util.futureToTask(MessagesDAO.postToRoom(roomId.toLong, m)) flatMap { message =>
        ...
      }
    }
```
Here `postToRoom` returns a `Future[...]`, which gets turned into a `Task[...]`. The following `flatMap` works just as it would on a `Future`, so we can keep working on building the response there.

### Slick with autoInc ids

I always had issues with model creation in Slick (at least Slick 1) when dealing with autoInc ids. Some solutions I've seen involve defining your models with an `Option[Long]` instead of just `Long` for the `id`, just to deal correctly with the case where the model hasn't been inserted yet and so doesn't have an id yet. Then you end up having to `.map` or even `.get` all over your codebase to use the id.

But, it turns out you can write your insert query in a way that works without this - [Thank you](http://sap1ens.com/blog/2015/07/26/scala-slick-3-how-to-start/)!
```
  def postToRoom(roomId: Long, message: MessageInput): Future[Message] = {
    // query to use AutoInc on id
    val q = messages returning messages.map(_.id) into ((m, id) => m.copy(id = id))
    val mess = Message(0, roomId, message.author, message.content, DateTime.now())

    db.run(q += mess)
  }
```
[Code here](https://github.com/jolleon/snack-chat/blob/master/src/main/scala/com/example/pokeathttp4s/models/Messages.scala).

### Websockets with scalaz topics

In order to deal with websockets in Http4s you'll need to create an `Exchange`. This is part of `scalaz-stream` so I won't go into these details but [this post](https://gist.github.com/djspiewak/d93a9c4983f63721c41c) was extremely helpful.


## Frontend

I figured this was a good time to try Backbone.js. This turned out to be a much easier framework to get started with than all the plumbing I had to do in the backend.

The framework makes a lot of sense, great [documentation](http://backbonejs.org/) and [this tutorial](arturadib.com/hello-backbonejs/) (not working at this time, looks like someone's having DNS issues) were all I needed.
