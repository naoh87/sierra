package org.sierra

import org.scalatest._
import org.sierra.command._
import redis.clients.jedis.Jedis

class ConnectionSpec extends FlatSpec with Matchers {

  it should "OK" in {
    implicit val jedis = new Jedis("localhost")
    jedis.flushAll()

    val StringData = Path("root").on("ok_spec") :: LongKey("a") :: Binary(StringValue)
    import api._

    StringData / 4 <<: Get() should be(None)
    StringData / 4 <<: SetBinary("aa") should be(true)
    StringData / 4 <<: Get() should be(Some("aa"))
    jedis.close()
  }

  "scodec" should "OK" in {
    import api._
    implicit val jedis = new Jedis("localhost")
    jedis.flushAll()

    import scodec._
    import codecs._

    case class Sample(a: Int, b: Int)
    val sampleEncoder = (int16 :: int16).as[Sample]

    val StringData = Path("root").on("ok_spec") :: LongKey("a") :: LongKey("b") :: Binary(ValueType(sampleEncoder))
    val path = StringData / 32 / 13

    path <<: Get() should be(None)
    path <<: SetBinary(Sample(12,1024)) should be(true)
    path <<: Get() should be(Some(Sample(12,1024)))
    jedis.close()
  }

  "integers" should "OK" in {
    import api._
    implicit val jedis = new Jedis("localhost")
    jedis.flushAll()

    case class HogeValue(a: Int, b: String)

    val hogeKey = PKey[HogeValue](m => s"{${m.a}}-${m.b}")

    val StringData = Path("root").on("ok_spec") :: LongKey("a") :: hogeKey :: Integers()
    val path = StringData / 32 / HogeValue(3, "hoge")

    path <<: Get() should be(None)
    path <<: SetBinary(1234L) should be(true)
    path <<: Incr()
    path <<: Get() should be(Some(1235L))
    jedis.close()
  }
}
