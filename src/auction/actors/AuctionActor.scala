package auction.actors

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import akka.actor.FSM
import akka.actor.actorRef2Scala
import auction.helpers.AuctionSystemLogger
import auction.helpers.auctionIsOver
import auction.helpers.bid
import auction.helpers.bidTimerExpired
import auction.helpers.createAuction
import auction.helpers.deleteTimerExpired
import auction.helpers.notifyWinner
import auction.helpers.registerAuction
import auction.helpers.auctionRegistered

class AuctionActor extends Actor with FSM[state, data] {

  val system = akka.actor.ActorSystem("system")
  import system.dispatcher

  private val CREATED_WITH_TIME: String = " Created with bid time "
  private val DELETE_TIME: String = " and with delete time "
  private val AND_PRICE: String = " and price "
  private val FIRST_BUYER: String = " First buyer "
  private val WITH_PRICE: String = " with price "
  private val BEST_BUYER: String = " best buyer "
  private val WINNER: String = " the winner is "
  private val AUCTION: String = "Auction#"
  private val TO_BEAT: String = " price to beat "
  private val BID_TIMER_STARTED = " Bid timer started "
  private val DELETE_TIMER_STARTED = " Delete timer started "
  private val BID_TIMER_EXPIRED = " Bid timer expired "
  private val DELETE_TIMER_EXPIRED = " Delete timer expired "
  private val AUCTION_IS_OVER = " Auction is over "

  private val NO_BEST_BID: Int = -1
  private val NO_BUYER_ID: Int = -1

  private var bidTimer: Int = 0
  private var deleteTimer: Int = 0
  private var auctionId: Int = -1;
  private var price: Int = -1;
  private var bestBuyerRef = Nil
  private var title: String = ""

  private val AuctionManager = context.parent

  private def startBidTimer() {
    AuctionSystemLogger.log(AUCTION + auctionId + title, BID_TIMER_STARTED + bidTimer)
    system.scheduler.scheduleOnce(Duration.create(bidTimer, TimeUnit.SECONDS), new Runnable {
      override def run = {
        AuctionSystemLogger.log(AUCTION + auctionId + title, BID_TIMER_EXPIRED)
        self ! bidTimerExpired()
      }
    })
  }

  private def startDeleteTimer() {
    AuctionSystemLogger.log(AUCTION + auctionId + title, DELETE_TIMER_STARTED + deleteTimer)
    system.scheduler.scheduleOnce(Duration.create(deleteTimer, TimeUnit.SECONDS), new Runnable {
      override def run = {
        self ! deleteTimerExpired()
      }
    })
  }

  /*
  * FSM 
  */

  startWith(AuctionOff, Uninitialized)

  when(AuctionOff) {
    case Event(createAuction(bidTimer, deleteTimer, price, auctionId, title), Uninitialized) => {
      this.deleteTimer = deleteTimer
      this.bidTimer = bidTimer
      this.price = price
      this.auctionId = auctionId
      this.title = title
      AuctionSystemLogger.log(AUCTION + auctionId + title, CREATED_WITH_TIME + bidTimer + DELETE_TIME + deleteTimer + AND_PRICE + price)
      context.actorSelection("akka://default/user/auctionManager/auctionSearch") ! registerAuction(auctionId, self)
      context.parent ! auctionRegistered()
      goto(AuctionCreated) using AuctionData(NO_BEST_BID, NO_BUYER_ID)
    }
    case Event(bid(bidPrice, bidBuyerId), Uninitialized) => {
      stay using Uninitialized
    }
  }

  when(AuctionCreated) {
    case Event(bid(bidPrice, buyerId), AuctionData(bestBid, NO_BUYER_ID)) => {
      if (bidPrice > price) {
        AuctionSystemLogger.log(AUCTION + auctionId + title, FIRST_BUYER + buyerId + WITH_PRICE + bidPrice + TO_BEAT + price)
        price = bidPrice
        goto(AuctionActivated) using AuctionData(bidPrice, buyerId)
      } else {
        stay using AuctionData(bestBid, NO_BUYER_ID)
      }
    }
    case Event(bidTimerExpired, AuctionData(bestBid, buyerId)) => {
      goto(AuctionIgnored) using AuctionData(bestBid, buyerId)
    }
  }

  when(AuctionActivated) {
    case Event(bid(bidPrice, bidBuyerId), AuctionData(bestBid, buyerId)) => {
      if (bidPrice > bestBid) {
        AuctionSystemLogger.log(AUCTION + auctionId + title, BEST_BUYER + bidBuyerId + WITH_PRICE + bidPrice)
        stay using AuctionData(bidPrice, bidBuyerId)
      } else {
        stay using AuctionData(bestBid, buyerId)
      }
    }
    case Event(bidTimerExpired(), AuctionData(bestBid, buyerId)) => {
      AuctionSystemLogger.log(AUCTION + auctionId + title, WINNER + buyerId + " with " + bestBid)
      AuctionManager ! notifyWinner(auctionId, buyerId)
      goto(AuctionSold) using AuctionData(bestBid, buyerId)
    }
  }

  when(AuctionIgnored) {
    case Event(deleteTimerExpired(), AuctionData(_, _)) => {
      AuctionSystemLogger.log(AUCTION + auctionId + title, DELETE_TIMER_EXPIRED + AUCTION_IS_OVER)
      AuctionManager ! auctionIsOver()
      stay
    }
    case Event(bid(_,_), AuctionData(_, _)) => {
      stay
    }
  }

  when(AuctionSold) {
    case Event(deleteTimerExpired(), AuctionData(_, _)) => {
      AuctionSystemLogger.log(AUCTION + auctionId + title, DELETE_TIMER_EXPIRED + AUCTION_IS_OVER)
      AuctionManager ! auctionIsOver()
      stay
    }
    case Event(bid(_,_), AuctionData(_, _)) => {
      stay
    }
  }

  onTransition {
    case AuctionOff -> AuctionCreated => {
      for ((_, AuctionData(bestBid, buyerId)) <- Some(stateData, nextStateData)) {
        startBidTimer()
      }
    }
    case AuctionCreated -> AuctionIgnored => {
      for ((_, AuctionData(bestBid, buyerId)) <- Some(stateData, nextStateData)) {
        startDeleteTimer()
      }
    }
    case AuctionActivated -> AuctionSold => {
      for ((_, AuctionData(bestBid, buyerId)) <- Some(stateData, nextStateData)) {
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