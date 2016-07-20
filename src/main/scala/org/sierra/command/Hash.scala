package org.sierra.command

import org.sierra.Path
import org.sierra.QPath
import org.sierra.ValueType
import redis.clients.jedis.Jedis
import shapeless.HNil

class Hash[A, B](path: Path, fieldType: ValueType[A], valueType: ValueType[B]) {
  def redisKey: Array[Byte] = path.value.getBytes()

  def encodeField(data: A): Array[Byte] = fieldType.encode(data)

  def encodeValue(data: B): Array[Byte] = valueType.encode(data)

  def bulkReplyValue(data: Array[Byte]): Option[B] = Option(data).map(valueType.decode)
}

case class HGet[A](field: A) extends RedisCommand2HKR[Hash, A, Option] {
  override def execute[K >: A, V](source: Hash[K, V])(implicit client: Jedis): Option[V] =
    source.bulkReplyValue(client.hget(source.redisKey, source.encodeField(field)))
}

case class HSet[A, B](field: A, value: B) extends RedisCommand2HKV[Hash, A, B, Long] {
  def execute[K >: A, V >: B](source: Hash[K, V])(implicit client: Jedis): Long =
    client.hset(source.redisKey, source.encodeField(field), source.encodeValue(value))
}

