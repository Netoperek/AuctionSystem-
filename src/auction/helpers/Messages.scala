package auction.helpers

import akka.actor.ActorRef

sealed trait AuctionManagamentMessage
sealed trait AuctionMessage
sealed trait TimerMessage
 
case class startAuctionSystem(numberOfAuctions: Int, numberOfBuyers: Int) extends AuctionManagamentMessage
case class closeAuctionSystem() extends AuctionManagamentMessage
case class createAuction(bidTimer: Int, deleteTime: Int, price: Int, auctionId: Int) extends AuctionManagamentMessage

case class bid(price: Int, buyerId: Int) extends AuctionMessage
case class startBidding(auctions: List[ActorRef], buyerId: Integer) extends AuctionMessage
case class stopBidding(auction: ActorRef) extends AuctionMessage
case class notifyWinner(buyerId: Int, auctionId: Int) extends AuctionMessage
case class youWon(auctionId: Int) extends AuctionMessage
case class auctionIsOver() extends AuctionMessage

case class bidTimerExpired() extends TimerMessage
case class deleteTimerExpired() extends TimerMessage

case class keepBidding(times: Int)