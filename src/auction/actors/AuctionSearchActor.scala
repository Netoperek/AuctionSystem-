package auction.actors

import auction.helpers.findAuction
import akka.actor.Actor
import auction.helpers.bidTimerExpired
import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.ActorRefFactory
import auction.helpers.sendFoundAuctions
import auction.helpers.registerAuction
import auction.helpers.AuctionSystemLogger
import auction.helpers.auctionRegistered
import auction.helpers.SystemSettings
import auction.helpers.GlobalState
import auction.helpers.TimeMeasure

class AuctionSearchActor extends Actor {

  private val SEARCH_ACTOR: String = "Search Actor"
  private val SEARCHING: String = "Searching auctions with keyword "
  private val AUCTION_REGISTER: String = " Auction Registered "

  def find(keyWord: String): List[SystemSettings.AuctionPrice] = {
    AuctionSystemLogger.log(SEARCH_ACTOR, SEARCHING + keyWord)
    val pattern = keyWord.r
    var result: List[SystemSettings.AuctionPrice] = List()
    SystemSettings.TITLES.
      filter(x => x._2 contains keyWord).
      foreach(x => {
        if (GlobalState.registerdAuctions.contains(x._1)) {
          result = result.+:(GlobalState.registerdAuctions(x._1))
        }
      })
    return result
  }

  def receive = {
    case findAuction(keyWord) => {
      val foundAuctions = find(keyWord)
      val start: Long = System.currentTimeMillis();
      sender ! sendFoundAuctions(foundAuctions)
      val stop: Long = System.currentTimeMillis();
      val result = start.toString()
      TimeMeasure.addResult(result)
    }
    case registerAuction(auctionId, auction, price) => {
      AuctionSystemLogger.log(SEARCH_ACTOR, AUCTION_REGISTER)
      val auctionPrice = SystemSettings.AuctionPrice(price, auction)
      GlobalState.registerdAuctions += auctionId -> auctionPrice
    }
  }

}