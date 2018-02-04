/**
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.garyaiki.dendrites.algebird.agent

import akka.agent.Agent
import com.twitter.algebird.{QTree, QTreeSemigroup}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag
import com.github.garyaiki.dendrites.algebird.{buildQTree, buildQTrees}
import com.github.garyaiki.dendrites.algebird.AlgebirdConfigurer.qTreeLevel
import com.github.garyaiki.dendrites.algebird.typeclasses.QTreeLike

/** Akka Agent for concurrently updating Qtree
  *
  * @deprecated
  * @constructor Creates Agent singleton for QTree
  * @tparam A: QTreeLike either BigDecimal, BigInt, Double, Float, Int or Long
  * @param name
  * @param xs optional initial values
  * @param ec implicit execution context for future
  * @param sg implicit QTreeSemigroup for adding
  *
  * @example [[com.github.garyaiki.dendrites.algebird.agent.stream.QTreeAgentFlow]]
  * @see [[http://doc.akka.io/api/akka/current/akka/agent/index.html Agent]]
  * @see [[http://twitter.github.io/algebird/datatypes/approx/q_tree.html QTree]
  * @author Gary Struthers
  */
class QTreeAgent[A: QTreeLike : TypeTag](val name: String = "", level: Int = qTreeLevel, xs: Option[Seq[A]] = None)
  (implicit ec: ExecutionContext, sg: QTreeSemigroup[A]) {

  val zero: QTree[A] = implicitly[QTreeLike[A]].apply(sg.underlyingMonoid.zero)

  val agent = xs match {
    case None => Agent(zero)
    case Some(xs) => Agent(buildQTree[A](xs))
  }

  /** Update agent with sequence of values
    *
    * @param xs Seq of BigDecimal, BigInt, Double, Float, Int or Long
    * @return future of new QTree after this and all pending updates
    */
  def alter(xs: Seq[A]): Future[QTree[A]] = {
    agent alter (oldState => {
      oldState match {
        case `zero` => buildQTree[A](xs)
        case _ => sg.sumOption(buildQTrees[A](xs) :+ oldState).get
      }
    })
  }
}
