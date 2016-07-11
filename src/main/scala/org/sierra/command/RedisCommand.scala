package org.sierra.command

import org.sierra.QPath
import redis.clients.jedis.Jedis
import shapeless.HNil


trait RedisCommand1[A, B] {
  def <<:(qp: QPath[HNil, A])(implicit client: Jedis): B = execute(qp.build)

  def on(qp: QPath[HNil, A])(implicit client: Jedis): B = execute(qp.build)

  def execute(source: A)(implicit client: Jedis): B
}


trait RedisCommand2[A, B] {
  def on(qp: QPath[HNil, A], qp2: QPath[HNil, A])(implicit client: Jedis): B = execute(qp.build, qp2.build)

  def execute(source: A, source2: A)(implicit client: Jedis): B
}