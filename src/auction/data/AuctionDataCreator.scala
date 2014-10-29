package auction.data

import auction.helpers.SystemSettings

object AuctionDataCreator {
  val models = List(" Audi ", " Mercedes ", " BMW ", " Fiat ", " Mustang ", " Ferrari ")
  val modelTypes = List(" A6 ", " Z1 ", " A7 ", " A8 ", " RX ", " A5 ")
  val engines = List(" diesel ", " petrol ", " gas ")
  val gearboxes = List(" automatic ", " manual ")

  def getRandomKeyWord(): String = {
    val number = randomNumber(0, 3)
    if (number == 0) return getRandomModel()
    if (number == 1) return getRandomModelType()
    if (number == 2) return getRandomEngine()
    if (number == 3) return getRandomGearbox()
    return "none"
  }

  private def getRandomModel(): String = {
    return models(randomNumber(0, models.length - 1))
  }

  private def getRandomModelType(): String = {
    return modelTypes(randomNumber(0, modelTypes.length - 1))
  }

  private def getRandomEngine(): String = {
    return engines(randomNumber(0, engines.length - 1))
  }

  private def getRandomGearbox(): String = {
    return gearboxes(randomNumber(0, gearboxes.length))
  }

  private val random = new scala.util.Random

  private def randomNumber(bottom: Int, top: Int): Int = {
    val range = bottom to top.-(1)
    return range(random.nextInt(range length))
  }

  def createAuctionsTitle: Map[Int, String] = {
    var auctionData: Map[Int, String] = Map()

    for (x <- 0 to SystemSettings.NUMBER_OF_AUCTIONS.-(1)) {
      auctionData += x -> (models(randomNumber(0, models.length - 1)).
        +(modelTypes(randomNumber(0, modelTypes.length - 1))).
        +(engines(randomNumber(0, engines.length - 1)))).
        +(gearboxes(randomNumber(0, gearboxes.length - 1)))
    }

    return auctionData
  }
}