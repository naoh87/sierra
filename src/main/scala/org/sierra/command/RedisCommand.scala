package org.sierra.command

import org.sierra.QPath
import redis.clients.jedis.Jedis
import shapeless.HNil

import scala.language.higherKinds


trait RedisCommand1[A, B] {
  def <<:(qp: QPath[HNil, A])(implicit client: Jedis): B = execute(qp.build)

  def on(qp: QPath[HNil, A])(implicit client: Jedis): B = execute(qp.build)

  def execute(source: A)(implicit client: Jedis): B
}

trait RedisCommand1m[A[_], B[_]] {
  def <<:[T](qp: QPath[HNil, A[T]])(implicit client: Jedis): B[T] = execute(qp.build)

  def on[T](qp: QPath[HNil, A[T]])(implicit client: Jedis): B[T] = execute(qp.build)

  def execute[T](source: A[T])(implicit client: Jedis): B[T]
}

trait RedisCommand1a[A[_], B] {
  def <<:[T](qp: QPath[HNil, A[T]])(implicit client: Jedis): B = execute(qp.build)

  def execute[T](source: A[T])(implicit client: Jedis): B
}

trait RedisCommand2[A, B] {
  def on(qp: QPath[HNil, A], qp2: QPath[HNil, A])(implicit client: Jedis): B = execute(qp.build, qp2.build)

  def execute(source: A, source2: A)(implicit client: Jedis): B
}