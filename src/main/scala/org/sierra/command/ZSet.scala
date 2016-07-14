package org.sierra.command

import java.lang
import java.util

import org.sierra.ValueType
import org.sierra.Path
import org.sierra.QPath
import org.sierra.QPathBuilder
import redis.clients.jedis.Jedis
import redis.clients.jedis.Pipeline
import redis.clients.jedis.Response
import shapeless.HNil

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.language.higherKinds
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
  def execute[T >: A](zset: ZSet[T])(implicit client: Jedis): Long =
    client.zcard(zset.path.redisKey.getBytes())

  def executeP[T >: A](zset: ZSet[T])(implicit pipe: PipeLiner): Future[Long] =
    pipe.to(_.zcard(zset.path.redisKey.getBytes())).map(_.toLong)(pipe.context)
}

class PipeLiner(client: Pipeline)(implicit val context: ExecutionContext) {
  private[this] val await = Promise[Unit]

  def sync() = {
    await.completeWith(Future {
      client.sync()
    })
  }

  def to[A](f: (Pipeline) => Response[A]): Future[A] = {
    val r = f(client)
    await.future.map(_ => r.get())
  }
}

case class ZAdd[A](score: Double, member: A) extends ZSetCommand[A, Long] {
  def execute[T >: A](source: ZSet[T])(implicit client: Jedis): Long =
    client.zadd(source.path.redisKey.getBytes, score, source.memberType.encode(member))
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


trait ZSetCommand[A, B] extends RedisCommand1K[ZSet, A, B]

trait ZSetCommandM[T[_]] extends RedisCommand1M[ZSet, T]

trait ZSetCommandA[B] extends RedisCommand1R[ZSet, B]
