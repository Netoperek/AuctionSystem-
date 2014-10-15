package auction.helpers

import akka.actor.ActorSystem
import akka.event.Logging
import akka.actor.Props
import akka.actor.actorRef2Scala

object ManagerInitializer {
  
  private val systemActor = ActorSystem()
  
  private def initAuctionManager(){
	  val auctionManager = systemActor.actorOf(Props[AuctionManager], "auctionManager")
	  auctionManager ! startAuctionSystem(SystemSettings.NUMBER_OF_AUCTIONS, SystemSettings.NUMBER_OF_BUYERS)
  }
  
  def main(args: Array[String]): Unit = initAuctionManager()

}