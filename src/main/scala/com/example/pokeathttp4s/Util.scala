package com.example.pokeathttp4s

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}
import scalaz.concurrent.Task
import scalaz.Scalaz._


object Util {
  def futureToTask[A](f: => Future[A])(implicit ec: ExecutionContext): Task[A] = {
    Task.async { cb =>
      f.onComplete {
        case Success(a) => cb(a.right)
        case Failure(t) => cb(t.left)
      }
    }
  }
}
