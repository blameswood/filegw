package com.hrocloud.tiangong.filegw.service.actors

import akka.actor._
import akka.io.IO
import spray.can.Http

class Master extends Actor with ActorLogging {

  val config = context.system.settings.config
  val fileHandler = context.actorOf(Props[FileHandler], name = "handler")

  override def preStart(): Unit = {
     implicit val system = context.system
     IO(Http) ! Http.Bind(fileHandler, "0.0.0.0", port = config.getInt("spray.port"))
   }

  override def receive = {
    case x =>
      log.info(s"$x received.")
  }

}