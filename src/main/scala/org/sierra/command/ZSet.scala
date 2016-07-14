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


case class ZRange(start: Int, end: Int) extends ZSetCommandM[Seq] {
  def execute[T](zset: ZSet[T])(implicit client: Jedis): Seq[T] =
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


case class ZPop() extends ZSetCommandM[Option] {
  def execute[T](source: ZSet[T])(implicit client: Jedis): Option[T] =
    Option(client.eval(
      """
        |local r = redis.call('zrange', KEYS[1], 0, 0)
        |if r then redis.call('zremrangebyrank', KEYS[1], 0, 0) end
        |return r
      """.stripMargin.getBytes(), 1, source.path.redisKey.getBytes())
      .asInstanceOf[util.List[Array[Byte]]])
      .flatMap(_.asScala.map(source.memberType.decode).headOption)
}

case class ZCount(
  min: Double = Double.NegativeInfinity,
  max: Double = Double.PositiveInfinity
) extends ZSetCommandA[Long] {
  def execute[A](source: ZSet[A])(implicit client: Jedis): Long =
    client.zcount(source.path.redisKey, min, max)
}


import scala.language.higherKinds

trait ZSetCommand[A, B] extends RedisCommand1[ZSet[A], B]

trait ZSetCommandM[T[_]] extends RedisCommand1m[ZSet, T]

trait ZSetCommandA[B] extends RedisCommand1a[ZSet, B]
