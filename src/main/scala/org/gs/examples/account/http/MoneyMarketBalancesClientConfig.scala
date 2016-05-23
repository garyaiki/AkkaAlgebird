package org.gs.examples.account.http

import akka.util.Timeout
import scala.concurrent.duration.MILLISECONDS
import org.gs.http._

/** Read config for Money Market Balances Client
  *
  * @author Gary Struthers
  *
  */
class MoneyMarketBalancesClientConfig() {

  val hostConfig = getHostConfig("dendrites.money-market-balances.http.interface",
    "dendrites.money-market-balances.http.port")
  val config = hostConfig._1
  val baseURL = configBaseUrl("dendrites.money-market-balances.http.path", hostConfig)
  val timeout = new Timeout(config.getInt("dendrites.money-market-balances.http.millis"),
      MILLISECONDS)
}
