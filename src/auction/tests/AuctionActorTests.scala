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

class AuctionActorTests extends TestKit(ActorSystem("AuctionSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfter {
  
  private val NO_BEST_BID : Int = -1
  private val NO_BUYER_ID : Int = -1
  private val TITLE : String = " some title "
  
  override def afterAll(): Unit = {
    system.shutdown()
  }
  
  var fsm = TestFSMRef(new AuctionActor) 

  before { 
    fsm = TestFSMRef(new AuctionActor)
  }

  "An FSM auction actor" when {
    "Is in initial state" when {
      "Nothing is done" must {
        "Have Uninitialized state" in {
		    assert(fsm.stateName == AuctionOff)
		    assert(fsm.stateData == Uninitialized)
      	}
	  }
      "Receives bid message" must {
        "Have AuctionOff state" in {
      	    fsm ! bid(1,1)
        	assert(fsm.stateName == AuctionOff)
        	assert(fsm.stateData == Uninitialized)
        }
      }
      "Receives createAuction message" must {
        "Have CreatedAuction state" in {
	        assert(fsm.stateName == AuctionOff)
		    assert(fsm.stateData == Uninitialized)
		    fsm ! createAuction(100, 100, 100, 1, "some title")
		    assert(fsm.stateName == AuctionCreated)
		    assert(fsm.stateData == AuctionData(NO_BEST_BID, NO_BUYER_ID))
        }
      }
    }
    
    "Is in auction created state" when {
      "Receives bid message" when {
        "Price is bigger than start price" must {
          "Have ActivatedAuction state with best Price and buyerId" in {
            fsm ! createAuction(100, 100, 100, 1, TITLE)
            val price = 1000;
            val buyerId = 3;
            fsm ! bid(price, buyerId)
            assert(fsm.stateName == AuctionActivated)
            assert(fsm.stateData == AuctionData(price,buyerId))
          }
        }
        "Price is lower than start price" must {
          "Have AutctionCreated state with no best bid and no buyerId" in {
            fsm ! createAuction(100, 100, 100, 1, TITLE)
            val price = 10;
            val buyerId = 10;
            fsm ! bid(price, buyerId)
            assert(fsm.stateName == AuctionCreated)
            assert(fsm.stateData == AuctionData(NO_BEST_BID, NO_BUYER_ID))
          }
        }
      }
      "Receives bidTimerExpired message" must {
        "Have AuctionIgored state and no best bid and no buyer id" in {
          fsm ! createAuction(100, 100, 100, 1, TITLE)
          fsm ! bidTimerExpired
          assert(fsm.stateName == AuctionIgnored)
          assert(fsm.stateData == AuctionData(NO_BEST_BID, NO_BUYER_ID))
        }
      }
    }
    
    "Is in auction activated state" when {
    	"Receives bid message" when {
    	  "Price is bigger than current best price" must {
    	    "Have AuctionAvtivated state and data with better price and buyer id" in {
    	      fsm ! createAuction(100, 100, 100, 1, TITLE)
    	      fsm ! bid(1000, 11)
    	      fsm ! bid(1001, 12)
    	      assert(fsm.stateName == AuctionActivated)
    	      assert(fsm.stateData == AuctionData(1001, 12))
    	    }
    	  }
    	  "Price is lower than current best price" must {
    	    "Have AuctionActivated state and data with same price and same buyer id" in {
    	      fsm ! createAuction(100, 100, 100, 1, TITLE)
    	      fsm ! bid(1000, 11)
    	      fsm ! bid(999, 9)
    	      assert(fsm.stateName == AuctionActivated)
    	      assert(fsm.stateData == AuctionData(1000, 11))
    	    }
    	  }
    	}
    	"Receives bidTimerExpired message" must {
    	  "Have AuctionSold state and best price and buyer id" in {
    		  fsm ! createAuction(100, 100, 100, 1, TITLE)
    	      fsm ! bid(1000, 11)
    	      fsm ! bid(1001, 12)
    	      fsm ! bidTimerExpired()
    	      assert(fsm.stateName == AuctionSold)
    	      assert(fsm.stateData == AuctionData(1001,12))
    	  }
    	}
    }
    
    "Is in auction ignored state" when {
      "Receives deleteTimerExpired message" must {
        "Have AuctionIgnored state and send message to Auction Manager that auction is over" in {
          fsm.setState(AuctionIgnored, AuctionData(1000,1))
          fsm ! deleteTimerExpired() 
          assert(fsm.stateName == AuctionIgnored)
        }
      }
    }
    
    "Is in auction sold state" when {
      "Receives deleteTimerExired message" must {
        "Have AuctionSold state and send message to Auction Manager that auction is over" in {
          fsm.setState(AuctionSold, AuctionData(1000,1))
          fsm ! deleteTimerExpired()
          assert(fsm.stateName == AuctionSold)
        }
      } 
    }
  }
}