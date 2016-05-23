/**
  */
package org.gs.examples

import scala.collection.immutable.Set

/** Functions for AccountType, the case objects that distinguish them and AccountBallances, their
  * optional list of account ids and balances
  *
  * @author Gary Struthers
  */
package object account {

  type AccBalances[A] = Option[List[(Long, A)]]

  val accountTypes: Set[AccountType] = Set(Checking, Savings, MoneyMarket)

  def isAccountBalances(e: Any): Boolean = e.isInstanceOf[AccountBalances]

  def isAccBalances[A](e: Any): Boolean = e.isInstanceOf[AccBalances[A]]

  def isAccountType(e: Any): Boolean = e.isInstanceOf[AccountType]

  /** Extract the 'A' balance values from AccBalances[A]
    * @param e Product is a supertype of AccBalances[A], throw exception if it isn't
    * @return List[List[A] call flatten to get List[A]
    */
  def extractBalances[A](e: Product): List[A] = {
    val i = e.productElement(0).asInstanceOf[AccBalances[A]]
    val j = i match {
      case Some(x) => x.map(y => Some(y._2))
      case None    => List(None)
    }
    j.flatten
  }

  /** Extract List[A] balances from AccountBalances
    * @param accountBalances is a Seq of subTypes of AccountBalances
    * @return Seq[List[Product]] call flatten to get List[A]
    */
  def extractBalancesLists[A](accountBalances: Seq[AnyRef]): Seq[List[A]] = {
    for (i <- accountBalances) yield {
      i match {
        case c: CheckingAccountBalances[A]    => extractBalances(c)
        case m: MoneyMarketAccountBalances[A] => extractBalances(m)
        case s: SavingsAccountBalances[A]     => extractBalances(s)
      }
    }
  }

  def extractBalancesVals[A](accountBalances: Seq[AnyRef]): Seq[A] = {
    val l = extractBalancesLists(accountBalances)
    l.flatten
  }
}