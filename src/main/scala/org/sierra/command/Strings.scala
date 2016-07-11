package org.sierra.command

import org.sierra.ValueType
import org.sierra.Path
import org.sierra.QPathBuilder
import redis.clients.jedis.Jedis
import shapeless.HNil

class Strings[A](path: Path, vType: ValueType[A]) {
  def redisKey: Array[Byte] = path.redisKey.getBytes()

  def encode(data: A): Array[Byte] = vType.encode(data)

  def bulkReply(data: Array[Byte]): Option[A] = Option(data).map(vType.decode)
}


object Strings {
  def apply[A](vType: ValueType[A]): QPathBuilder[HNil, Strings[A]] =
    QPathBuilder(HNil, Strings(_, vType))

  def apply[A](pos: Path, vType: ValueType[A]) =
    new Strings(pos, vType)
}

trait StringsCommand[A, B] extends RedisCommand1[Strings[A], B]


case class Get[A]() extends StringsCommand[A, Option[A]] {
  override def execute(source: Strings[A])(implicit client: Jedis): Option[A] =
    source bulkReply (client get source.redisKey)
}

case class SetBinary[A](value: A) extends StringsCommand[A, Boolean] {
  override def execute(source: Strings[A])(implicit client: Jedis): Boolean =
    Option(client.set(source.redisKey, source.encode(value))).contains("OK")
}

