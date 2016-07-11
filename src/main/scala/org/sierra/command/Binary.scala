package org.sierra.command

import org.sierra.StringInteger
import org.sierra.ValueType
import org.sierra.Path
import org.sierra.QPathBuilder
import redis.clients.jedis.Jedis
import shapeless.HNil

class Binary[A](path: Path, vType: ValueType[A]) {
  def redisKey: Array[Byte] = path.redisKey.getBytes()

  def encode(data: A): Array[Byte] = vType.encode(data)

  def bulkReply(data: Array[Byte]): Option[A] = Option(data).map(vType.decode)
}


object Binary {
  def apply[A](vType: ValueType[A]): QPathBuilder[HNil, Binary[A]] =
    QPathBuilder(HNil, Binary(_, vType))

  def apply[A](pos: Path, vType: ValueType[A]) =
    new Binary(pos, vType)
}

trait BinaryCommand[A, B] extends RedisCommand1[Binary[A], B]


case class Get[A]() extends BinaryCommand[A, Option[A]] {
  override def execute(source: Binary[A])(implicit client: Jedis): Option[A] =
    source bulkReply (client get source.redisKey)
}

case class SetBinary[A](value: A) extends BinaryCommand[A, Boolean] {
  override def execute(source: Binary[A])(implicit client: Jedis): Boolean =
    Option(client.set(source.redisKey, source.encode(value))).contains("OK")
}

case class GetSet[A](value: A) extends BinaryCommand[A, Option[A]] {
  override def execute(source: Binary[A])(implicit client: Jedis): Option[A] =
    source bulkReply client.getSet(source.redisKey, source.encode(value))
}


class Integers(path: Path) extends Binary[Long](path, StringInteger)

trait IntegerCommand[B] extends BinaryCommand[String, B]

case class Incr() extends IntegerCommand[Long] {
  override def execute(source: Binary[String])(implicit client: Jedis): Long =
    client.incr(source.redisKey)
}

case class IncrBy(increment: Long) extends IntegerCommand[Long] {
  override def execute(source: Binary[String])(implicit client: Jedis): Long =
    client.incrBy(source.redisKey, increment)
}