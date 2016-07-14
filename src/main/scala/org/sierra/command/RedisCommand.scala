package org.sierra.command

import org.sierra.Path
import org.sierra.QPath
import redis.clients.jedis.Jedis
import shapeless.HNil

import scala.language.higherKinds

trait RedisCommand0K[R] {
  def execute(path: Path)(implicit client: Jedis): R
}

trait RedisCommand1S[M, R] {
  def <<:(qp: QPath[HNil, M])(implicit client: Jedis): R = execute(qp.build)

  def on(qp: QPath[HNil, M])(implicit client: Jedis): R = execute(qp.build)

  def execute(source: M)(implicit client: Jedis): R
}

trait RedisCommand1M[M[_], R[_]] {
  def <<:[T](qp: QPath[HNil, M[T]])(implicit client: Jedis): R[T] = execute(qp.build)

  def on[T](qp: QPath[HNil, M[T]])(implicit client: Jedis): R[T] = execute(qp.build)

  def execute[T](source: M[T])(implicit client: Jedis): R[T]
}

trait RedisCommand1R[M[_], R] {
  def <<:[T](qp: QPath[HNil, M[T]])(implicit client: Jedis): R = execute(qp.build)

  def execute[T](source: M[T])(implicit client: Jedis): R
}

trait RedisCommand1K[M[_], +E, R] {
  // define this method at implicit class for inttelij scala paraser
  def <<:[K >: E](qp: QPath[HNil, M[K]])(implicit client: Jedis): R = execute(qp.build)

  def execute[K >: E](source: M[K])(implicit client: Jedis): R
}

trait RedisCommand1KM[M[_], +E, R[_]] {
  // define this method at implicit class for inttelij scala paraser
  def <<:[K >: E](qp: QPath[HNil, M[K]])(implicit client: Jedis): R[K] = execute(qp.build)

  def execute[K >: E](source: M[K])(implicit client: Jedis): R[K]
}


trait RedisCommand2HKV[M[_, _], +E1, +E2, R] {
  // define this method at implicit class for inttelij scala paraser
  def <<:[K >: E1, V >: E2](qp: QPath[HNil, M[K, V]])(implicit client: Jedis): R = execute(qp.build)

  def execute[K >: E1, V >: E2](source: M[K, V])(implicit client: Jedis): R
}


trait RedisCommand2HKR[M[_, _], +E1, R[_]] {
  // define this method at implicit class for inttelij scala paraser
  def <<:[K >: E1, V](qp: QPath[HNil, M[K, V]])(implicit client: Jedis): R[V] = execute(qp.build)

  def execute[K >: E1, V](source: M[K, V])(implicit client: Jedis): R[V]
}
