package auction.tests

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpec
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.actor.Props
import auction.actors.AuctionActor
import org.scalatest.WordSpecLike
import akka.testkit.TestFSMRef
import auction.actors.AuctionOff
import auction.actors.Uninitialized
import auction.helpers.createAuction
import auction.actors.AuctionCreated
import auction.actors.AuctionData
import org.scalatest.BeforeAndAfter
import auction.actors.AuctionActivated
import auction.helpers.bid
import auction.actors.AuctionIgnored
import auction.helpers.bidTimerExpired
import auction.helpers.bidTimerExpired
import auction.actors.AuctionSold
import auction.helpers.deleteTimerExpired
import auction.helpers.auctionIsOver
import auction.helpers.deleteTimerExpired
import auction.helpers.AuctionManager
import auction.helpers.startAuctionSystem
import akka.testkit.{ TestActors, DefaultTimeout, ImplicitSender, TestKit }
import scala.concurrent.duration._
import scala.collection.immutable
import auction.helpers.AuctionSystemOff
import auction.helpers.AuctionSystemLogger
import auction.helpers.SystemSettings
import auction.helpers.AuctionSystemOn

class SystemTest extends TestKit(ActorSystem("AuctionSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfter {

  private val NO_BEST_BID: Int = -1
  private val NO_BUYER_ID: Int = -1
  private val TITLE: String = " some title "

  override def afterAll(): Unit = {
    system.shutdown()
  }

  val systemActor = ActorSystem()
  var fsm = TestFSMRef(new AuctionManager)

  "SystemOff" when {
    "Start System" must {
      "Finish without errors, and buyers must have won auctions with proper titles" in {
        fsm ! startAuctionSystem()
        assert(fsm.stateName == AuctionSystemOn)
        within(SystemSettings.TIMERS_TOP_TIME.*(2) seconds) {
          expectNoMsg();
          assert(fsm.stateName == AuctionSystemOff);
          assert(AuctionSystemLogger.resultsMap.filter(x => !(SystemSettings.TITLES(x._1) contains x._2.split(" ")(2))).isEmpty)
        }
      }
    }
  }
}