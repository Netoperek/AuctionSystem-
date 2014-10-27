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

  private var titles: Map[Int, String] = Map()
  private var path: String = "akka://AuctionSearchSpec/user/$$a/auction1"
  private var registerdAuctions: Map[Integer, ActorRef] = Map()
  private val AUCTION_REGISTER: String = " Auction Registered "
  private val SEARCH_ACTOR: String = "Search Actor"
  private val SEARCHING: String = "Searching auctions with keyword "

  def find(keyWord: String): List[ActorRef] = {
    AuctionSystemLogger.log(SEARCH_ACTOR, SEARCHING + keyWord)
    val pattern = keyWord.r
    var result: List[ActorRef] = List()
    for ((auctionId, title) <- SystemSettings.TITLES) {
      val foundPattern = pattern.findFirstIn(title)
      if (foundPattern != None) {
        result = result.+:(registerdAuctions(auctionId))
      }
    }
    return result
  }

  def receive = {
    case findAuction(keyWord) => {
      val foundAuctions = find(keyWord)
      sender ! sendFoundAuctions(foundAuctions)
    }
    case registerAuction(auctionId, auction) => {
      AuctionSystemLogger.log(SEARCH_ACTOR, AUCTION_REGISTER)
      registerdAuctions += auctionId -> auction
    }
  }

}