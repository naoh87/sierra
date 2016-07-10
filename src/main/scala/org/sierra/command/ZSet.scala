package org.sierra.command

import java.util

import org.sierra.ValueType
import org.sierra.Path
import org.sierra.QPath
import org.sierra.QPathBuilder
import redis.clients.jedis.Jedis
import shapeless.HNil

import scala.collection.JavaConverters._


class ZSet[A](val path: Path, val memberType: ValueType[A])

object ZSet {
  def apply[A](memberType: ValueType[A]): QPathBuilder[HNil, ZSet[A]] =
    QPathBuilder(HNil, ZSet(_, memberType))

  def apply[A](pos: Path, memberType: ValueType[A]) = new ZSet(pos, memberType)
}


case class ZRange[A](start: Int, end: Int) extends ZSetCommand[A, Seq[A]] {
  def execute(zset: ZSet[A])(implicit client: Jedis): Seq[A] =
    Option(client.zrange(zset.path.redisKey.getBytes, start, end)).toSeq
      .flatMap(_.asScala.toSeq.map(zset.memberType.decode))
}

case class ZCard[A](member: A) extends ZSetCommand[A, Long] {
  def execute(zset: ZSet[A])(implicit client: Jedis): Long =
    client.zcard(zset.path.redisKey.getBytes())
}


case class Zadd[A](score: Double, member: A) extends ZSetCommand[A, Long] {
  def execute(zset: ZSet[A])(implicit client: Jedis): Long =
    client.zadd(zset.path.redisKey.getBytes, score, zset.memberType.encode(member))
}

case class Zpop[A](member: A) extends ZSetCommand[A, Option[A]] {
  def execute(zset: ZSet[A])(implicit client: Jedis): Option[A] =
    Option(client.eval(
      """
         |local result = redis.call('zrange', KEYS[1], -1, -1)
         |if result then redis.call('zremrangebyrank', KEYS[1], -1, -1) end
         |return result
      """.stripMargin, 1, zset.path.redisKey))
      .map(_.asInstanceOf[util.Set[Array[Byte]]])
      .flatMap(_.asScala.map(zset.memberType.decode).headOption)
}

trait ZSetCommand[A, B] extends RedisCommand1[ZSet[A], B]
