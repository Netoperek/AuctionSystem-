package auction.actors

import akka.actor.Actor
import auction.helpers.exhibitAuctions
import auction.helpers.createAuction
import auction.helpers.SystemSettings
import auction.helpers.AuctionSystemLogger
import akka.actor.Props
import auction.helpers.registerAuction

class SellerActor extends Actor {

  private var sellerId = -1;

  private val SELLER: String = "Seller#"
  private val EXHIBIT: String = " exhibiting auctions "

  private val random = new scala.util.Random

  private def randomTime(): Int = {
    val range = SystemSettings.TIMERS_BOTTOM_TIME to SystemSettings.TIMERS_TOP_TIME
    return range(random.nextInt(range length))
  }

  private def randomPrice(): Int = {
    val range = SystemSettings.AUCTION_BOTTOM_PRICE to SystemSettings.AUCTION_TOP_PRICE
    return range(random.nextInt(range length))
  }

  def receive = {
    case exhibitAuctions(auctions, sellerId) => {
      this.sellerId = sellerId
      AuctionSystemLogger.log(SELLER + sellerId, EXHIBIT)
      auctions.zipWithIndex foreach {
        case (auction, auctionId) => {
          auction ! createAuction(randomTime(), randomTime(), randomPrice(), auctionId, SystemSettings.TITLES(auctionId))
        }
      }
    }
  }

}