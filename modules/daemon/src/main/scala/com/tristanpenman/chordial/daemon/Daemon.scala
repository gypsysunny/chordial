package com.tristanpenman.chordial.daemon

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.tristanpenman.chordial.core.Event
import com.tristanpenman.chordial.daemon.service.WebSocketServer
import spray.can.Http
import spray.can.server.UHttp
import scala.concurrent.duration._
import scala.io.StdIn

object Daemon extends App {

  implicit val system = ActorSystem("chordial-daemon")

  implicit val timeout: Timeout = 3.seconds

  // Generate IDs ranging from 0 to 359 (inclusive) so that when visualising the network,
  // each node can be represented as a one degree arc on the ring
  private val idModulus = 360

  // Create an actor that is responsible for creating and terminating nodes, while ensuring
  // that nodes are assigned unique IDs in the Chord key-space
  private val governor = system.actorOf(Governor.props(idModulus), "Governor")

  // Create a web server that will provide both a simple RESTful API for creating and
  // terminating nodes, and a WebSocket interface through which events will be published
  private val server = system.actorOf(WebSocketServer.props(governor), "WebSocketServer")

  // Subscribe the WebSocketServer actor to events published by nodes
  system.eventStream.subscribe(server, classOf[Event])

  // Start the web server
  private val httpPortNumber = 4567
  IO(UHttp) ? Http.Bind(server, "localhost", httpPortNumber)
  StdIn.readLine("Hit ENTER to exit ...\n")
  system.shutdown()
  system.awaitTermination()
}
