package auction.actors

import akka.actor.Actor
import auction.helpers._
import auction.helpers.AuctionManagamentMessage
import akka.actor.FSM
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

class AuctionActor extends Actor with FSM[state, data] {
  
  val system = akka.actor.ActorSystem("system")
  import system.dispatcher

  private val CREATED_WITH_TIME : String = " Created with bid time "
  private val DELETE_TIME : String = " and with delete time "
  private val AND_PRICE : String = " and price "
  private val FIRST_BUYER : String = " First buyer "
  private val WITH_PRICE : String = " with price "
  private val BEST_BUYER : String = " best buyer "
  private val WINNER : String = " the winner is "
  private val AUCTION : String = "Auction#"
  private val TO_BEAT : String = " price to beat "
  private val UNHANDLED_MSG : String = "Error - unhandled message"
  private val BID_TIMER_STARTED = " Bid timer started"
  private val DELETE_TIMER_STARTED = " Delete timer started"
  private val BID_TIMER_EXPIRED = " Bid timer expired "
  private val DELETE_TIMER_EXPIRED = " Delete timer expired "

  private val NO_BEST_BID : Int = -1
  private val NO_BUYER_ID : Int = -1

  private var bidTimer : Int = 0
  private var deleteTimer : Int = 0
  private var auctionId : Int = -1;
  private var price : Int = -1;
  
  private def startBidTimer() {
    AuctionSystemLogger.log(AUCTION  + auctionId, BID_TIMER_STARTED)
    system.scheduler.scheduleOnce(Duration.create(bidTimer, TimeUnit.SECONDS), new Runnable {
      override def run = self ! bidTimerExpired()
    })
  }
  
  private def startDeleteTimer() {
    AuctionSystemLogger.log(AUCTION  + auctionId, DELETE_TIMER_STARTED)
    system.scheduler.scheduleOnce(Duration.create(deleteTimer, TimeUnit.SECONDS), new Runnable {
      override def run = self ! deleteTimerExpired()
    })
  }
  
  private def setAuctionId(auctionId: Int) {
    this.auctionId = auctionId
  }
  
  private def setPrice(price: Int) {
    this.price = price
  }
  
  private def setBidTimer(bidTimer : Int) {
    this.bidTimer = bidTimer
  }
  
  private def setDeleteTimer(deleteTimer : Int) {
    this.deleteTimer = deleteTimer
  }
  
  def getPrice() : Int = {
    return price;
  }
  

  
  /*
  * FSM 
  */

  startWith(AuctionOff, Uninitialized)
  
  when(AuctionOff) {
    case Event(createAuction(bidTimer, deleteTimer, price, auctionId), Uninitialized) => {
    	setDeleteTimer(deleteTimer)
    	setBidTimer(bidTimer)
    	setAuctionId(auctionId)
    	setPrice(price)
    	AuctionSystemLogger.log(AUCTION  + auctionId, CREATED_WITH_TIME + bidTimer + DELETE_TIME + deleteTimer + AND_PRICE + price)
    	goto(AuctionCreated) using AuctionData(NO_BEST_BID, NO_BUYER_ID)
    }
    case Event(bid(bidPrice, bidBuyerId), AuctionData(_, _)) => {
    	stay using AuctionData(NO_BEST_BID, NO_BUYER_ID)
    }
  }
  
  when(AuctionCreated) {
    case Event(bid(bidPrice, buyerId), AuctionData(bestBid, NO_BUYER_ID)) => {
      if(bidPrice > price) {
    	AuctionSystemLogger.log(AUCTION  + auctionId, FIRST_BUYER + buyerId + WITH_PRICE + bidPrice + TO_BEAT + price)
    	price = bidPrice
        goto(AuctionActivated) using AuctionData(bidPrice, buyerId)
      } else {
        stay using AuctionData(bestBid, NO_BUYER_ID)
      }
    }
    case Event(bidTimerExpired, AuctionData(bestBid, buyerId)) => {
      AuctionSystemLogger.log(AUCTION  + auctionId, BID_TIMER_EXPIRED)
      goto(AuctionIgnored) using AuctionData(bestBid, buyerId)
    }
  }
  
  when(AuctionActivated) {
  	case Event(bid(bidPrice, bidBuyerId), AuctionData(bestBid, buyerId)) => {
      if(bidPrice >= bestBid) {
    	AuctionSystemLogger.log(AUCTION  + auctionId, BEST_BUYER + bidBuyerId + WITH_PRICE + bidPrice)
    	sender ! stopBidding(self)
        stay using AuctionData(bidPrice, bidBuyerId)
      } else {
        stay using AuctionData(bestBid, buyerId)
      }
    }
  	case Event(bidTimerExpired(), AuctionData(bestBid, buyerId)) => {
  	  AuctionSystemLogger.log(AUCTION  + auctionId, BID_TIMER_EXPIRED)
  	  AuctionSystemLogger.log(AUCTION  + auctionId, WINNER + buyerId + " with " + bestBid)
  	  goto(AuctionSold) using AuctionData(bestBid, buyerId)
  	}
  }
  
  when(AuctionIgnored) {
    case Event(deleteTimerExpired, AuctionData(_, _)) => {
        AuctionSystemLogger.log(AUCTION  + auctionId ," Delete timer expired ")
    	stay
    }
  }
  
  when(AuctionSold) {
    case Event(deleteTimerExpired, AuctionData(_, _)) => {
      AuctionSystemLogger.log(AUCTION  + auctionId ," Delete timer expiredd ")
      stay
    }
  }
  
  whenUnhandled {
    case Event(e, s) => {
      AuctionSystemLogger.log(AUCTION + auctionId, UNHANDLED_MSG)
      stay
    }
  }
  
  onTransition {
    case AuctionOff -> AuctionCreated => {
      for((_, AuctionData(bestBid, buyerId)) <- Some(stateData, nextStateData)) {
    	  startBidTimer()
      }
    }
    case AuctionCreated -> AuctionIgnored => {
      for((_, AuctionData(bestBid, buyerId)) <- Some(stateData, nextStateData)) {
    	  startDeleteTimer()
      }
    }
    case AuctionActivated -> AuctionSold => {
      for((_, AuctionData(bestBid, buyerId)) <- Some(stateData, nextStateData)) {
    	  startDeleteTimer()
      }
    }
  }

}

sealed trait state;
case object AuctionCreated extends state;
case object AuctionIgnored extends state;
case object AuctionActivated extends state;
case object AuctionOff extends state;
case object AuctionSold extends state;

sealed trait data;
case class AuctionData(bestBid: Int, buyerId: Int) extends data
case object Uninitialized extends data