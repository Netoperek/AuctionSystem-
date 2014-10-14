package auction.helpers

object AuctionSystemLogger {
  def log(from:String, msg: String) = {
    println("[" + from + "] " + msg)
  }
}