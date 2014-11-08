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
  private val BEATEN_HIGHER = " My offer got beaten, bidding higher with "
  private val BEATEN_OUT = " My offer got beaten, to high for me - stop bidding "
  private val BIDDING_AUCTION = "bidding auction "

  private val random = new scala.util.Random
  private val auctionManager = context.parent

  private var buyerId = -1
  private var auctions: List[SystemSettings.AuctionPrice] = Nil
  private var keyWord: String = "NONE"
  private var myMaxPrice: Int = -1

  private def setMyMaxPrice() = {
    val range = SystemSettings.BUYER_MIN_PRICE to SystemSettings.BUYER_MAX_PRICE
    myMaxPrice = range(random.nextInt(range length))
  }

  private def randomPrice(bottomPrice: Int): Int = {
    val range = bottomPrice to myMaxPrice
    if (range.length == 0) return 0
    return range(random.nextInt(range length))
  }

  private def randomAuction(bottom: Int, top: Int): Int = {
    val range = bottom to top.-(1)
    return range(random.nextInt(range length))
  }

  private def bidChosenAuctions() = {
    self ! keepBidding(0)
  }

  def receive = {
    case startBidding(auctions: List[ActorRef], buyerId: Integer) => {
      setMyMaxPrice()
      this.buyerId = buyerId
      val keyWord = AuctionDataCreator.getRandomKeyWord()
      this.keyWord = keyWord
      context.actorSelection(context.parent.path + SystemSettings.ACTOR_SELECTION_SEARCH) ! findAuction(keyWord)
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
    case keepBidding(auctionToBid: Int) => {
      if (auctions.length != 0) {
        val auction = auctions(auctionToBid)
        val price = randomPrice(auction.price)
        auctions(auctionToBid).auction ! bid(price, buyerId)
        AuctionSystemLogger.log(BUYER + buyerId + " " + keyWord, BIDDING_AUCTION + auctionToBid + " with " + price)
        if (auctionToBid > auctions.length.-(1)) self ! keepBidding(auctionToBid.+(1))
      }
    }
    case yourOfferIsWorse(auction, auctionId, currentPrice, buyerId) => {
      val price = randomPrice(currentPrice)
      if (currentPrice < price) {
        AuctionSystemLogger.log(BUYER + buyerId + " " + keyWord, BEATEN_HIGHER + price + " > " + currentPrice)
        self ! stopBidding(auction)
      } else {
        AuctionSystemLogger.log(BUYER + buyerId + " " + keyWord, BEATEN_OUT + currentPrice + " >= " + myMaxPrice)
        auction ! bid(price, buyerId)
      }
    }
  }

}