package org.sierra

import org.sierra.command.RedisCommand1K
import redis.clients.jedis.Jedis
import shapeless.HList
import shapeless.HNil


case class Path(value: String) {
  def /(key: String): Path = Path(value + ":" + key)

  def on(key: String): Path = Path(value + ":" + key)
}


case class LongKey(prefix: String = "") extends QKey[Long] {
  override def toKey(q: Long): String = prefix + q.toString
}

case class PKey[A](f: A => String) extends QKey[A] {
  def toKey(q: A): String = f(q)
}

trait QKey[T] {
  def toKey(q: T): String
}

case class QPath[+H <: HList, +M](path: Path, qKeys: H, invoker: Path => M, prepend: Seq[String]) {
  def build: M = invoker(prepend.headOption.filter(_.nonEmpty).fold(path)(path.on))
}

case class QPathBuilder[+A <: HList, +M](hList: A, invoker: (Path) => M, prepend: Seq[String] = Seq("")) {
  def ::[B](qKey: QKey[B]): QPathBuilder[shapeless.::[QKey[B], A], M] =
    QPathBuilder(qKey :: hList, invoker, Seq("") ++ prepend)

  def ::(path: Path): QPath[A, M] =
    QPath(path, hList, invoker, prepend)

  def ::(key: String) =
    QPathBuilder(hList, invoker, prepend = Seq(prepend.headOption.filter(_.nonEmpty).fold(key)(key + ":" + _)) ++ prepend.tail)
}

package object api {

  import scala.language.higherKinds

  implicit class QPathResolver[T, +H <: HList, K](qp: QPath[shapeless.::[QKey[T], H], K]) {
    def /(q: T): QPath[H, K] =
      QPath(
        qp.prepend.headOption.filter(_.nonEmpty).fold(qp.path)(qp.path.on).on(qp.qKeys.head.toKey(q)),
        qp.qKeys.tail,
        qp.invoker,
        qp.prepend.tail
      )
  }

  implicit class RedisCommandIntelijHelper[M[_], C, B](command: RedisCommand1K[M, C, B]) {
    def <<:[K >: C](qp: QPath[HNil, M[K]])(implicit client: Jedis): B = on(qp)

    def on[K >: C](qp: QPath[HNil, M[K]])(implicit client: Jedis): B = command.execute(qp.build)
  }

}
