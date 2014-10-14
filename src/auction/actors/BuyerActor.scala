package auction.actors

import akka.actor.Actor
import auction.helpers._
import java.util.regex.Pattern.Begin
import akka.actor.ActorRef
import akka.actor.FSM
import akka.actor.Props

class BuyerActor extends Actor {
  
  private final val BID = " bidding with price "
  private final val BUYER = "Buyer#"
  private final val AUCTION = " Auction#"
  private final val STOPPED = " stopped bidding "
  
  private val BOTTOM_PRICE : Int = 10
  private val TOP_PRICE : Int = 100
  private val BOTTOM_BID : Int = 10
  private val TOP_BID : Int = 30
  
  private val random = new scala.util.Random
  
  private var buyerId = -1
  private var auctions : List[ActorRef] = Nil
  private var keepBidding = false
  
  private def setBuyerId(buyerId: Integer) {
    this.buyerId = buyerId
  }
  
  private def setAuctions(auctions: List[ActorRef]){
    this.auctions = auctions
  }
  
  private def numberOfBids() : Int = {
    val range = BOTTOM_BID to TOP_BID
    return range(random.nextInt(range length))   
  }

  private def randomPrice() : Int = {
    val range = BOTTOM_PRICE to TOP_PRICE
    return range(random.nextInt(range length))
  }

  private def randomAuction(bottom: Int, top: Int) : Int = {
    val range = bottom to top.-(1)
    return range(random.nextInt(range length))
  }
  
  private def bidRandomAuction() = {
    for( i <- 1 to numberOfBids() ){
	    val auctionId = randomAuction(0, auctions.length)
	    val price = randomPrice()
	    val auction = auctions(auctionId)
	    //AuctionSystemLogger.log(BUYER + buyerId + BID + price + AUCTION + auctionId)
	    auctions(auctionId) ! bid(price, buyerId)
	    Thread sleep 1000
    }
  }

  def receive = {
    case startBidding(auctions: List[ActorRef], buyerId: Integer) => {
      setBuyerId(buyerId)
      setAuctions(auctions)
      bidRandomAuction()
    }
    case stopBidding(auction: ActorRef) => {
      auctions = auctions.diff(List(auction))
    }
  }

}