package auction.helpers

object AuctionSystemLogger {

  var resultsMap: Map[Int, String] = Map()

  def log(from: String, msg: String) = {
    println("[" + from + "] " + msg)
  }

  def addAuctionsResults(buyer: String, auctionId: Int) = {
    resultsMap += auctionId -> buyer
  }

  def logResults() = {
    println()
    println("--------------------------------------------------------")
    println("-----------------Auctions Result------------------------")
    println()
    for ((auctionId, buyer) <- resultsMap) {
      println(buyer + " [" + SystemSettings.TITLES(auctionId) + "] " + "(" + auctionId + ")")
    }
    println()
    println("--------------------------------------------------------")
    println("--------------------------------------------------------")
    println()
  }
}