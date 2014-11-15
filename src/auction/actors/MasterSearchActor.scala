package auction.actors

import akka.actor.Actor
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router
import auction.helpers.AuctionSystemLogger
import auction.helpers.GlobalState
import auction.helpers.SystemSettings
import auction.helpers.findAuction
import auction.helpers.registerAuction
import akka.routing.BroadcastRoutingLogic

class MasterSearchActor extends Actor {

  private val MASTER_SEARCH: String = "MasterSearch"

  var router = {
    val routees = Vector.fill(SystemSettings.NUMER_OF_SEARCH_ACTORS) {
      val auctionSearch = context.actorOf(Props[AuctionSearchActor])
      context watch auctionSearch
      ActorRefRoutee(auctionSearch)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: findAuction => {
      Router(BroadcastRoutingLogic(), router.routees)
      router.route(w, sender())
    }
    case Terminated(actorRef) =>
      router = router.removeRoutee(actorRef)
      val r = context.actorOf(Props[AuctionSearchActor])
      context watch r
      router = router.addRoutee(r)
    case w: registerAuction => {
      Router(RoundRobinRoutingLogic(), router.routees)
      router.route(w, sender())
    }
  }

}