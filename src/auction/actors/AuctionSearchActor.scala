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

class AuctionSearchActor extends Actor {

  private var registerdAuctions: Map[Integer, SystemSettings.AuctionPrice] = Map()
  private val AUCTION_REGISTER: String = " Auction Registered "
  private val SEARCH_ACTOR: String = "Search Actor"
  private val SEARCHING: String = "Searching auctions with keyword "

  def find(keyWord: String): List[SystemSettings.AuctionPrice] = {
    AuctionSystemLogger.log(SEARCH_ACTOR, SEARCHING + keyWord)
    val pattern = keyWord.r
    var result: List[SystemSettings.AuctionPrice] = List()
    SystemSettings.TITLES.
      filter(x => x._2 contains keyWord).
      foreach(x => result = result.+:(registerdAuctions(x._1)))
    return result
  }

  def receive = {
    case findAuction(keyWord) => {
      val foundAuctions = find(keyWord)
      sender ! sendFoundAuctions(foundAuctions)
    }
    case registerAuction(auctionId, auction, price) => {
      AuctionSystemLogger.log(SEARCH_ACTOR, AUCTION_REGISTER)
      val auctionPrice = SystemSettings.AuctionPrice(price, auction)
      registerdAuctions += auctionId -> auctionPrice
    }
  }

}