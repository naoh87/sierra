package org.sierra

import java.nio.charset.Charset

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.sierra.command._
import scodec.codecs._

class ZSetSpec extends FlatSpec with Matchers {

  trait Setup extends RedisSetup {

    import api._

    val sampleEncoder = (int16 :: string(Charset.defaultCharset())).as[HogeValue]
    val StringData = Path("root").on("ok_spec") :: LongKey("a") :: ZSet(ValueType(sampleEncoder))
    val path = StringData / 32

    case class HogeValue(a: Int, b: String)

  }


  "zpop" should "pop last" in new Setup {
    redis { implicit client =>

      path <<: Zadd(3, HogeValue(1, "aa")) should be(1)
      path <<: Zadd(4, HogeValue(1, "bb")) should be(1)
      path <<: Zadd(5, HogeValue(1, "kk")) should be(1)
      path <<: ZRange(2, 2) should be(Seq(HogeValue(1, "kk")))
      path <<: ZPop() should be(Some(HogeValue(1, "kk")))

      ZPop() on path

    }
  }

}
