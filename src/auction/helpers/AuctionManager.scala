package auction.helpers

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.FSM
import akka.actor.Props
import akka.actor.actorRef2Scala
import auction.actors.AuctionActor
import auction.actors.AuctionSearchActor
import auction.actors.BuyerActor
import auction.actors.SellerActor
import auction.actors.MasterSearchActor

class AuctionManager extends Actor with FSM[State, Data] {

  private val SYSTEM_NOT_STARTED: String = "System is not started yet"
  private val SYSTEM_STARTED: String = "System started"
  private val AUCTIONS_CREATED: String = "Auctions created"
  private val SYSTEM_CLOSED: String = "System closed"
  private val SYSTEM_RUNNING: String = "System is already running"
  private val WITH: String = " with "
  private val AND: String = "and "
  private val AUCTIONS: String = " auctions "
  private val BUYERS: String = " buyers "
  private val SELLERS: String = " sellers "
  private val AUCTION_SYSTEM: String = "Auction System"
  private val AUCTIONS_REGISTERED: String = " Auctions Registered, buyers can start bidding "
  private val NUMBER_OF_AUCTIONS_REGISTERED: String = " Number of auctions registered "

  private val random = new scala.util.Random
  private var auctionsRegistered = 1
  private var auctionsFinished = 0

  val auctionSearch = context.actorOf(Props[AuctionSearchActor], "auctionSearch")
  val masterSearch = context.actorOf(Props[MasterSearchActor], "masterSearch")

  private def initAuctions(numberOfAuctions: Int): List[ActorRef] = {
    return (1 to numberOfAuctions).map(num => context.actorOf(Props[AuctionActor], "auction" + num)).toList
  }

  private def initBuyers(numberOfBuyers: Int): List[ActorRef] = {
    return (1 to numberOfBuyers).map(num => context.actorOf(Props[BuyerActor], "buyer" + num)).toList
  }

  private def initSellers(numberOfSellers: Int): List[ActorRef] = {
    return (1 to numberOfSellers).map(num => context.actorOf(Props[SellerActor], "seller" + num)).toList
  }

  private def randomTime(): Int = {
    val range = SystemSettings.TIMERS_BOTTOM_TIME to SystemSettings.TIMERS_TOP_TIME
    return range(random.nextInt(range length))
  }

  private def randomPrice(): Int = {
    val range = SystemSettings.AUCTION_BOTTOM_PRICE to SystemSettings.AUCTION_TOP_PRICE
    return range(random.nextInt(range length))
  }

  private def prepareAuctionsForSeller(from: Int, auctions: List[ActorRef]): Map[ActorRef, String] = {
    var result: Map[ActorRef, String] = Map()
    var auctionId = from
    for (auction <- auctions) {
      result += auction -> SystemSettings.TITLES(auctionId)
      auctionId += 1
    }
    return result
  }

  /*
  * FSM 
  */

  startWith(AuctionSystemOff, Uninitialized)

  when(AuctionSystemOff) {
    case Event(startAuctionSystem(), Uninitialized) => {
      val auctionsList = initAuctions(SystemSettings.NUMBER_OF_AUCTIONS)
      val buyersList = initBuyers(SystemSettings.NUMBER_OF_BUYERS)
      val sellersList = initSellers(SystemSettings.NUMBER_OF_SELLERS)
      AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_STARTED +
        WITH +
        SystemSettings.NUMBER_OF_AUCTIONS +
        AUCTIONS +
        AND +
        SystemSettings.NUMBER_OF_BUYERS +
        BUYERS +
        SystemSettings.NUMBER_OF_SELLERS +
        SELLERS)
      goto(AuctionSystemOn) using AuctionSystemData(auctionsList, buyersList, sellersList)
    }
    case Event(closeAuctionSystem, Uninitialized) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_NOT_STARTED)
      stay using AuctionSystemData(Nil, Nil, Nil)
    }
  }

  when(AuctionSystemOn) {
    case Event(startAuctionSystem(),
      AuctionSystemData(auctionsList, buyersList, sellersList)) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_RUNNING)
      stay using AuctionSystemData(auctionsList, buyersList, sellersList)
    }
    case Event(notifyWinner(auctionId, buyerId), AuctionSystemData(auctionsList, buyersList, sellersList)) => {
      buyersList(buyerId) ! youWon(auctionId)
      stay using AuctionSystemData(auctionsList, buyersList, sellersList)
    }
    case Event(yourOfferIsWorse(auction, auctionId, currentPrice, buyerId), AuctionSystemData(auctionsList, buyersList, sellersList)) => {
      buyersList(buyerId) ! yourOfferIsWorse(auction, auctionId, currentPrice, buyerId)
      stay using AuctionSystemData(auctionsList, buyersList, sellersList)
    }
    case Event(auctionRegistered(), AuctionSystemData(auctionsList, buyersList, sellersList)) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, NUMBER_OF_AUCTIONS_REGISTERED + auctionsRegistered)
      if (auctionsRegistered == SystemSettings.NUMBER_OF_AUCTIONS) {
        AuctionSystemLogger.log(AUCTION_SYSTEM, AUCTIONS_REGISTERED)
        buyersList.zipWithIndex foreach {
          case (buyer, buyerId) => buyer ! startBidding(auctionsList, buyerId)
        }
      } else {
        auctionsRegistered += 1
      }

      stay using AuctionSystemData(auctionsList, buyersList, sellersList)
    }
    case Event(auctionIsOver(), AuctionSystemData(auctionsList, buyersList, sellersList)) => {
      auctionsFinished += 1
      if (auctionsFinished == SystemSettings.NUMBER_OF_AUCTIONS) {
        AuctionSystemLogger.logResults()
        self ! closeAuctionSystem()
      }
      buyersList.foreach {
        x => x ! stopBidding(sender)
      }
      stay using AuctionSystemData(auctionsList, buyersList, sellersList)
    }
    case Event(closeAuctionSystem(), AuctionSystemData(_, _, _)) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_CLOSED)
      TimeMeasure.writeResultsToFile();
      goto(AuctionSystemOff) using AuctionSystemData(Nil, Nil, Nil)
    }
  }

  onTransition {
    case AuctionSystemOff -> AuctionSystemOn => {
      for ((Uninitialized, AuctionSystemData(auctions, buyers, sellers)) <- Some(stateData, nextStateData)) {
        val numberOfAuctionsPerSeller = SystemSettings.NUMBER_OF_AUCTIONS./(SystemSettings.NUMBER_OF_SELLERS)
        val rest = SystemSettings.NUMBER_OF_AUCTIONS.%(SystemSettings.NUMBER_OF_SELLERS)
        var from: Int = 0
        var until: Int = numberOfAuctionsPerSeller
        sellers.zipWithIndex foreach {
          case (seller, sellerId) =>
            seller ! exhibitAuctions(prepareAuctionsForSeller(from, auctions.slice(from, until)), sellerId, from)
            from += numberOfAuctionsPerSeller
            until += numberOfAuctionsPerSeller
        }
        sellers.last ! exhibitAuctions(prepareAuctionsForSeller(from, auctions.slice(from, auctions.length)), sellers.length.-(1), from)
      }
    }
  }

}

sealed trait State
case object AuctionSystemOn extends State
case object AuctionSystemOff extends State

sealed trait Data
case object Uninitialized extends Data
case class AuctionSystemData(auctions: List[ActorRef], buyers: List[ActorRef], sellers: List[ActorRef]) extends Data