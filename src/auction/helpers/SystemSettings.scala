package auction.helpers

import auction.data.AuctionDataCreator
import akka.actor.ActorRef

object SystemSettings {
  val BUYER_BOTTOM_PRICE: Int = 10
  val BUYER_TOP_PRICE: Int = 100
  // max price that he will bid will be set once for all auctions
  val BUYER_MAX_PRICE: Int = 90
  val BUYER_MIN_PRICE: Int = 80
  val BUYER_BOTTOM_NUMBER_OF_BIDS: Int = 10
  val BUYER_TOP_NUMBER_OF_BIDS: Int = 30
  val TIMERS_BOTTOM_TIME: Int = 10
  val TIMERS_TOP_TIME: Int = 15
  val AUCTION_BOTTOM_PRICE: Int = 10
  val AUCTION_TOP_PRICE: Int = 200
  val NUMBER_OF_AUCTIONS = 100;
  val NUMBER_OF_BUYERS = 100;
  val NUMBER_OF_SELLERS = 3
  val TITLES: Map[Int, String] = AuctionDataCreator.createAuctionsTitle;
  val ACTOR_SELECTION_SEARCH = "/masterSearch"
  val NUMER_OF_SEARCH_ACTORS: Int = 10
  val RESULTS_OUTPUT_FILE = "results.txt"

  case class AuctionPrice(price: Int, auction: ActorRef)
}