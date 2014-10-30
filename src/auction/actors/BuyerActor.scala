package auction.actors

import akka.actor.Actor
import auction.helpers._
import java.util.regex.Pattern.Begin
import akka.actor.ActorRef
import akka.actor.FSM
import akka.actor.Props
import auction.data.AuctionDataCreator

class BuyerActor extends Actor {

  private val BID = " bidding with price "
  private val BUYER = "Buyer#"
  private val AUCTION = " Auction#"
  private val STOPPED = " stopped bidding "
  private val I_WON = " I won auction "
  private val COOL = " - thats cool !"
  private val FOUND = " AuctionSearchActor found "
  private val AUCTIONS = " auctions for me "

  private val random = new scala.util.Random
  private val auctionManager = context.parent

  private var buyerId = -1
  private var auctions: List[ActorRef] = Nil
  private var keyWord: String = "NONE"
  private var myTopPrice: Int = 0

  private val auctionSearchActor = context.actorOf(Props[AuctionSearchActor], "auctionSearch")

  private def numberOfBids(): Int = {
    val range = SystemSettings.BUYER_BOTTOM_NUMBER_OF_BIDS to SystemSettings.BUYER_TOP_NUMBER_OF_BIDS
    return range(random.nextInt(range length))
  }

  private def randomPrice(): Int = {
    val range = SystemSettings.BUYER_BOTTOM_PRICE to SystemSettings.BUYER_TOP_PRICE
    return range(random.nextInt(range length))
  }

  private def randomAuction(bottom: Int, top: Int): Int = {
    val range = bottom to top.-(1)
    return range(random.nextInt(range length))
  }

  private def bidChosenAuctions() = {
    self ! keepBidding(numberOfBids())
  }

  def receive = {
    case startBidding(auctions: List[ActorRef], buyerId: Integer) => {
      this.buyerId = buyerId
      val keyWord = AuctionDataCreator.getRandomKeyWord()
      this.keyWord = keyWord
      context.actorSelection("akka://default/user/auctionManager/auctionSearch") ! findAuction(keyWord)
    }
    case stopBidding(auction: ActorRef) => {
      auctions = auctions.diff(List(auction))
    }
    case youWon(auctionId: Int) => {
      AuctionSystemLogger.log(BUYER + buyerId + " " + keyWord, I_WON + auctionId + COOL)
      AuctionSystemLogger.addAuctionsResults(BUYER + buyerId + " " + keyWord, auctionId)
    }
    case sendFoundAuctions(auctions) => {
      this.auctions = auctions
      AuctionSystemLogger.log(BUYER + buyerId + " " + keyWord, FOUND + auctions.length + AUCTIONS)
      bidChosenAuctions()
    }
    case keepBidding(times: Int) => {
      if (auctions.length != 0) {
        Thread sleep SystemSettings.BUYER_BID_FREQUENCY
        val auctionId = randomAuction(0, auctions.length)
        val price = randomPrice()
        val auction = auctions(auctionId)
        auctions(auctionId) ! bid(price, buyerId)
        AuctionSystemLogger.log(BUYER + buyerId + " " + keyWord, "bidding auction " + auctionId + " with " + price)
        if (times > 0) self ! keepBidding(times.-(1))
      }
    }
  }

}