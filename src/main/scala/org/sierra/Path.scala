package org.sierra

import shapeless.HList


case class Path(redisKey: String) {
  def /(key: String): Path = Path(redisKey + ":" + key)
  def on(keys: String*): Path = Path((redisKey +: keys).mkString(":"))
}


case class LongKey(prefix: String = "") extends QKey[Long] {
  override def toKey(q: Long): String = prefix + ":" + q.toString
}

trait QKey[T] {
  def toKey(q: T): String
}

case class QPath[+H <: HList, +M](path: Path, qKeys: H, invoker: Path => M) {
  def build: M = invoker(path)
}

case class QPathBuilder[+A <: HList, +M](hList: A, invoker: (Path) => M) {
  def ::[B](qKey: QKey[B]): QPathBuilder[shapeless.::[QKey[B], A], M] =
    QPathBuilder(qKey :: hList, invoker)

  def ::(path: Path): QPath[A, M] =
    QPath(path, hList, invoker)
}

package object api {
  implicit class QPathResolver[T, +H <: HList, K](qp: QPath[shapeless.::[QKey[T], H], K]) {
    def /(q: T) =
      QPath(
        qp.path./(qp.qKeys.head.toKey(q)),
        qp.qKeys.tail,
        qp.invoker)
  }
}
