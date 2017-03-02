package info.gianlucacosta.twobingame.io.actors

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Object handling the local actor system
  */
object Actors {
  private var _actorSystem: ActorSystem = _

  def actorSystem: ActorSystem =
    _actorSystem


  def start(): Unit = {
    _actorSystem =
      ActorSystem("TwoBinGame")
  }


  def stop(): Unit = {
    Await.ready(
      _actorSystem.terminate(),
      Duration.Inf
    )
  }
}
