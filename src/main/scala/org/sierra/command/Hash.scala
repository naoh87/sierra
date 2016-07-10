package org.sierra.command

import org.sierra.Path
import org.sierra.ValueType
import redis.clients.jedis.Jedis

class Hash[A, B](path: Path, fieldType: ValueType[A], valueType: ValueType[B]) {
  def redisKey: Array[Byte] = path.redisKey.getBytes()

  def encodeField(data: A): Array[Byte] = fieldType.encode(data)

  def encodeValue(data: B): Array[Byte] = valueType.encode(data)

  def bulkReplyValue(data: Array[Byte]): Option[B] = Option(data).map(valueType.decode)
}

case class Hget[A, B](field: A) extends HashCommand[A, B, Option[B]] {
  override def execute(source: Hash[A, B])(implicit client: Jedis): Option[B] =
    source.bulkReplyValue(client.hget(source.redisKey, source.encodeField(field)))
}

case class Hset[A, B](field: A, value: B) extends HashCommand[A, B, Long] {
  override def execute(source: Hash[A, B])(implicit client: Jedis): Long =
    client.hset(source.redisKey, source.encodeField(field), source.encodeValue(value))
}

trait HashCommand[A, C, B] extends RedisCommand1[Hash[A, C], B]
