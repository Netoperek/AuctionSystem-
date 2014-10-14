package auction.helpers

import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.FSM
import akka.actor.ActorRef
import auction.actors.AuctionActor
import akka.dispatch.Foreach
import auction.actors.BuyerActor

class AuctionManager extends Actor with FSM[State, Data]{
  
  private val SYSTEM_NOT_STARTED : String = "System is not started yet"
  private val SYSTEM_STARTED : String = "System started"
  private val AUCTIONS_CREATED : String = "Auctions created"
  private val SYSTEM_CLOSED : String = "System closed"
  private val SYSTEM_RUNNING : String = "System is already running"
  private val UNHANDLED_MSG : String = "Error - unhandled message"
  private val WITH : String = " with "
  private val AND : String = "and "
  private val AUCTIONS : String = " auctions "
  private val BUYERS : String = " buyers "
  private val AUCTION_SYSTEM : String = "Auction System"
    
  private val BOTTOM_TIME : Int = 10
  private val TOP_TIME : Int = 15
  private val BOTTOM_PRICE : Int = 10
  private val TOP_PRICE : Int = 100
    
  private val random = new scala.util.Random
    
  private def initAuctions(numberOfAuctions: Int) : List[ActorRef] = {
	return (1 to numberOfAuctions).map(num => context.actorOf(Props[AuctionActor], "auction" + num)).toList
  }
  
  private def initBuyers(numberOfBuyers: Int) : List[ActorRef] = {
	return (1 to numberOfBuyers).map(num => context.actorOf(Props[BuyerActor], "buyer" + num)).toList
  }
  
  private def randomTime() : Int = {
    val range = BOTTOM_TIME to TOP_TIME
    return range(random.nextInt(range length))
  }
  
  private def randomPrice() : Int = {
    val range = BOTTOM_PRICE to TOP_PRICE
    return range(random.nextInt(range length))
  }
 


  /*
  * FSM 
  */ 

  startWith(AuctionSystemOff, Uninitialized)
  
  when(AuctionSystemOff) {
    case Event(startAuctionSystem(numberOfAuctions, numberOfBuyers), Uninitialized) => {
    	val auctionsList = initAuctions(numberOfAuctions)
    	val buyersList = initBuyers(numberOfBuyers)
    	AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_STARTED + WITH + numberOfAuctions + AUCTIONS + AND + numberOfBuyers + BUYERS)
    	goto(AuctionSystemOn) using AuctionSystemData(auctionsList, buyersList) 
    }
    case Event(closeAuctionSystem, Uninitialized) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_NOT_STARTED)
      stay using AuctionSystemData(Nil, Nil)
    }
  }

  when(AuctionSystemOn) {
    case Event(startAuctionSystem(numberOfAuctions, numberOfBuyers), 
    			AuctionSystemData(auctionsList, buyersList)) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_RUNNING)
      stay using AuctionSystemData(auctionsList, buyersList)
    }
    case Event(closeAuctionSystem, _) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, SYSTEM_CLOSED)
      goto(AuctionSystemOff) using AuctionSystemData(Nil, Nil) 
    }
  }

  whenUnhandled {
    case Event(e, s) => {
      AuctionSystemLogger.log(AUCTION_SYSTEM, UNHANDLED_MSG)
      stay
    }
  }

  onTransition {
    case AuctionSystemOff -> AuctionSystemOn => {
      for((Uninitialized, AuctionSystemData(auctions, buyers)) <- Some(stateData, nextStateData)) {

        auctions.zipWithIndex foreach { 
          case(auction, auctionId) => auction ! createAuction(randomTime(), randomTime(), randomPrice(), auctionId) 
        }
        
        buyers.zipWithIndex foreach { 
          case(buyer, buyerId) => buyer ! startBidding(auctions, buyerId) 
        }
        
      }
      	
    }
  }

}

sealed trait State
case object AuctionSystemOn extends State
case object AuctionSystemOff extends State

sealed trait Data
case object Uninitialized extends Data
case class AuctionSystemData(auctions: List[ActorRef], buyers: List[ActorRef]) extends Data