package org.sierra

import redis.clients.jedis.Jedis


trait RedisSetup {
  def redis[A](f: Jedis => A): A = {
    val jedis = new Jedis("localhost")
    jedis.flushAll()
    val r = f(jedis)
    jedis.close()
    r
  }
}
