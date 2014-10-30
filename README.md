Auction System

Acution system written in scala with Akka Actors.

The idea is to demonstrate system of creating, exhibiting, bidding and selling auctions by many buyers, sellers and auctions concurrently.

There are the following parts of the system:

1) ManagerInitializer which initializes whole system
2) AuctionManager which supports communication between auction actors and buyers actors which is a Finite State Machine
3) AuctionActor which represents auction which actually is a Finite State Machine
4) BuyerActor which represents buyers who bid chosen auctions
5) SellerActor which creates and exhibits auctions
6) AuctionSearchActor which can search auctions by their titles
7) SystemSettings where values like prices, timers and so on could be configured

How it works (very briefly) :

1)	AuctionManager can be in to states AuctionSystemOn or AuctionSystemOff.
	On Transition from AuctionSystemOff -> AuctionSystemOn it creates AuctionActors, BuyerActors, SellerActors, AuctioSearchActor.

2)	Sellers create and exhibit auctions. Every auction has title like " Audi A6 diesel automatic ".

	Every auction registers in AcutionSearchActor. When every auction is registered in AuctionSearchActor, AuctioSearchActor
	notifies AuctionManager that all auctions are registered and bidding can start.
	
3)	Buyers search auctions by title. For example a buyer may look for all auctions with Audi title. AuctioSearchActor finds
	wanted auctions and pass them to a buyer.

4)	Buyers start bidding and then the auction BID TIMER starts.

5)	When BID TIM expire AuctionActor tells which buyer won.

6) Finally the results of all auctions are printed