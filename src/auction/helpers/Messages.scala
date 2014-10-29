package auction.helpers

import akka.actor.ActorRef

sealed trait AuctionManagamentMessage
sealed trait AuctionMessage
sealed trait TimerMessage
sealed trait SellerMessage
sealed trait SearchMessage

case class startAuctionSystem() extends AuctionManagamentMessage
case class closeAuctionSystem() extends AuctionManagamentMessage
case class createAuction(bidTimer: Int, deleteTime: Int, price: Int, auctionId: Int, title: String) extends AuctionManagamentMessage
case class winnerNotified() extends AuctionManagamentMessage

case class bid(price: Int, buyerId: Int) extends AuctionMessage
case class startBidding(auctions: List[ActorRef], buyerId: Integer) extends AuctionMessage
case class stopBidding(auction: ActorRef) extends AuctionMessage
case class notifyWinner(buyerId: Int, auctionId: Int) extends AuctionMessage
case class youWon(auctionId: Int) extends AuctionMessage
case class auctionIsOver() extends AuctionMessage

case class bidTimerExpired() extends TimerMessage
case class deleteTimerExpired() extends TimerMessage

case class keepBidding(times: Int)

case class exhibitAuctions(auctions: Map[ActorRef, String], sellerId: Int, from: Int) extends SellerMessage
case class findAuction(keyWord: String) extends SearchMessage
case class sendFoundAuctions(auctions: List[ActorRef]) extends SearchMessage
case class registerAuction(auctionId: Integer, auction: ActorRef) extends SearchMessage
case class auctionRegistered() extends SearchMessage