package com.hrocloud.tiangong.filegw.service

import org.slf4j.LoggerFactory
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import akka.actor.{ActorSystem, Props}
import javax.annotation.{PostConstruct, PreDestroy}

import com.hrocloud.tiangong.filegw.service.actors.Master
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service


/**
 * FileServiceBean
 *
 * @author Sean Gao
 */
@Service
class FileServiceBean extends ApplicationListener[org.springframework.context.ApplicationEvent] {
  val logger = LoggerFactory.getLogger(classOf[FileServiceBean])

  var loggerConfig = new java.util.ArrayList[String](); loggerConfig.add("akka.event.slf4j.Slf4jLogger")

  val config = ConfigFactory.load("config").withValue("akka.loggers", ConfigValueFactory.fromIterable(loggerConfig))
  val system = ActorSystem("file-service", config)

  logger.info("FileServiceBean constructed.")

  @PostConstruct
  def makeActors(): Unit = {
    logger.info("FileServiceBean is being started. Making actors...")
//    Initializer.init()
    system.actorOf(Props[Master], "master")
  }

  @PreDestroy
  def shutdown(): Unit = {
    logger.info("FileServiceBean is being stopped. Shutting down actor system...")
    system.shutdown()
  }

  // the following is necessary to have spring initialize this bean on startup
  override def onApplicationEvent(ae: org.springframework.context.ApplicationEvent): Unit = {}

}
