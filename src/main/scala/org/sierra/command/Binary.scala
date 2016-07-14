package org.sierra.command

import org.sierra._
import redis.clients.jedis.Jedis
import shapeless.HNil
import scala.language.higherKinds

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

trait BinaryCommandM[R[_]] extends RedisCommand1M[Binary, R]

trait BinaryCommandK[A, R] extends RedisCommand1K[Binary, A, R]

trait BinaryCommandKM[A, R[_]] extends RedisCommand1KM[Binary, A, R]


case class Get() extends BinaryCommandM[Option] {
  def execute[T](source: Binary[T])(implicit client: Jedis): Option[T] =
    source bulkReply (client get source.redisKey)
}


case class SetBinaryParametric[A] private[command](value: A, nxxx: String, ex: Option[Int]) extends RedisCommand1K[Binary, A, Boolean] {
  override def execute[K >: A](source: Binary[K])(implicit client: Jedis): Boolean = ex match {
    case Some(x) =>
      Option(client.set(source.redisKey, source.encode(value), nxxx.getBytes(), "ex".getBytes(), x)).contains("OK")
    case None =>
      Option(client.set(source.redisKey, source.encode(value), nxxx.getBytes())).contains("OK")
  }
  def ex(expire: Int): SetBinaryParametric[A] = this.copy(ex = Some(expire))
}

case class Sets[A](value: A) extends RedisCommand1K[Binary, A, Boolean] {
  override def execute[K >: A](source: Binary[K])(implicit client: Jedis): Boolean =
    Option(client.set(source.redisKey, source.encode(value))).contains("OK")

  def nx() = new SetBinaryParametric(value, "nx", None)

  def xx() = new SetBinaryParametric(value, "xx", None)
}

case class SetNX[A](value: A) extends RedisCommand1K[Binary, A, Boolean] {
  override def execute[K >: A](source: Binary[K])(implicit client: Jedis): Boolean =
    Option(client.setnx(source.redisKey, source.encode(value))).contains(1)
}

case class SetEX[A](value: A, seconds: Int) extends RedisCommand1K[Binary, A, Boolean] {
  override def execute[K >: A](source: Binary[K])(implicit client: Jedis): Boolean =
    Option(client.setex(source.redisKey, seconds, source.encode(value))).contains("OK")
}

case class GetSet[A](value: A) extends RedisCommand1KM[Binary, A, Option] {
  def execute[K >: A](source: Binary[K])(implicit client: Jedis): Option[K] =
    source bulkReply client.getSet(source.redisKey, source.encode(value))
}


class Integers(path: Path) extends Binary[Long](path, StringInteger)

object Integers {
  def apply(): QPathBuilder[HNil, Integers] =
    QPathBuilder(HNil, apply)

  def apply(pos: Path): Integers = new Integers(pos)
}

trait IntegerCommand[B] extends RedisCommand1S[Integers, B]

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

trait StringsCommand[B] extends RedisCommand1S[Strings, B]

case class GetRange(start: Int, end: Int) extends StringsCommand[String] {
  override def execute(source: Strings)(implicit client: Jedis): String =
    new String(client.getrange(source.redisKey, start, end))
}

case class SetRange(offset: Int, value: String) extends StringsCommand[Long] {
  override def execute(source: Strings)(implicit client: Jedis): Long =
    client.setrange(source.redisKey, offset, value.getBytes())
}