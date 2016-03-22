package com.example.pokeathttp4s

import org.http4s.server.blaze.BlazeBuilder

object Server extends App {
  BlazeBuilder.bindHttp(5000)
    .mountService(HelloWorld.service, "/")
    .run
    .awaitShutdown()
}
