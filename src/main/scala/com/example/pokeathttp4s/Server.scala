package com.example.pokeathttp4s

import org.http4s.server.blaze.BlazeBuilder

object Server extends App {
  val port = sys.env.getOrElse("PORT", "5000").toInt

  BlazeBuilder.bindHttp(port, "0.0.0.0")
    .mountService(HelloWorld.service, "/")
    .run
    .awaitShutdown()
}
