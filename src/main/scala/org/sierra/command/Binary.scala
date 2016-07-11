package org.sierra.command

import org.sierra._
import redis.clients.jedis.Jedis
import shapeless.HNil

class Binary[A](path: Path, vType: ValueType[A]) {
  def redisKey: Array[Byte] = path.redisKey.getBytes()

  def encode(data: A): Array[Byte] = vType.encode(data)

  def bulkReply(data: Array[Byte]): Option[A] = Option(data).map(vType.decode)
}


object Binary {
  def apply[A](vType: ValueType[A]): QPathBuilder[HNil, Binary[A]] =
    QPathBuilder(HNil, apply(_, vType))

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

case class SetNX[A](value: A) extends BinaryCommand[A, Boolean] {
  override def execute(source: Binary[A])(implicit client: Jedis): Boolean =
    Option(client.setnx(source.redisKey, source.encode(value))).contains(1)
}

case class SetEX[A](value: A, seconds: Int) extends BinaryCommand[A, Boolean] {
  override def execute(source: Binary[A])(implicit client: Jedis): Boolean =
    Option(client.setex(source.redisKey, seconds, source.encode(value))).contains(1)
}

case class SetEXNX[A](value: A, seconds: Int) extends BinaryCommand[A, Boolean] {
  override def execute(source: Binary[A])(implicit client: Jedis): Boolean =
    Option(client.set(source.redisKey, "NX".getBytes(), "EX".getBytes(), source.encode(value), seconds)).contains("OK")
}

case class SetEXXX[A](value: A, seconds: Int) extends BinaryCommand[A, Boolean] {
  override def execute(source: Binary[A])(implicit client: Jedis): Boolean =
    Option(client.set(source.redisKey, "XX".getBytes(), "EX".getBytes(), source.encode(value), seconds)).contains("OK")
}

case class GetSet[A](value: A) extends BinaryCommand[A, Option[A]] {
  override def execute(source: Binary[A])(implicit client: Jedis): Option[A] =
    source bulkReply client.getSet(source.redisKey, source.encode(value))
}


class Integers(path: Path) extends Binary[Long](path, StringInteger)

object Integers {
  def apply(): QPathBuilder[HNil, Integers] =
    QPathBuilder(HNil, apply)

  def apply(pos: Path): Integers = new Integers(pos)
}

trait IntegerCommand[B] extends RedisCommand1[Integers, B]

case class Incr() extends IntegerCommand[Long] {
  override def execute(source: Integers)(implicit client: Jedis): Long =
    client.incr(source.redisKey)
}

case class IncrBy(increment: Long) extends IntegerCommand[Long] {
  override def execute(source: Integers)(implicit client: Jedis): Long =
    client.incrBy(source.redisKey, increment)
}

case class Decr() extends IntegerCommand[Long] {
  override def execute(source: Integers)(implicit client: Jedis): Long =
    client.decr(source.redisKey)
}

case class DecrBy(decrement: Long) extends IntegerCommand[Long] {
  override def execute(source: Integers)(implicit client: Jedis): Long =
    client.decrBy(source.redisKey, decrement)
}

class Strings(path: Path) extends Binary[String](path, StringValue)

object Strings {
  def apply(): QPathBuilder[HNil, Strings] =
    QPathBuilder(HNil, apply)

  def apply(pos: Path): Strings = new Strings(pos)
}

trait StringsCommand[B] extends BinaryCommand[String, B]

case class GETRANGE(start: Int, end: Int) extends StringsCommand[String] {
  def execute(source: Binary[String])(implicit client: Jedis): String =
    new String(client.getrange(source.redisKey, start, end))
}

case class SETRANGE(offset: Int, value: String) extends StringsCommand[Long] {
  override def execute(source: Binary[String])(implicit client: Jedis): Long =
    client.setrange(source.redisKey, offset, value.getBytes())
}