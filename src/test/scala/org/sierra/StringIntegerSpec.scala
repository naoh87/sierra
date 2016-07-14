package org.sierra

import java.nio.charset.Charset

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.sierra.command._
import scodec.codecs._

class StringIntegerSpec extends FlatSpec with Matchers {

  import api._
  trait Setup extends RedisSetup {

    val StringData = Path("root").on("ok_spec") :: LongKey("sint") :: ZSet(StringInteger)
    val path = StringData / 32
  }


  "string integer" should "pop last" in new Setup {
    redis { implicit client =>
      val intCommand = Zadd(3, 1)
      val longCommand = Zadd(3, 1L)

      path <<: Zadd(3, 1L) should be(1)
      path <<: Zadd(4, 2L) should be(1)
      path <<: Zadd(5, 3L) should be(1)
      path <<: ZRange(2, 2) should be(Seq(3L))
      path <<: ZCount() should be(3)
      path <<: ZPop() should be(Some(1L))
      path <<: ZCount() should be(2)

    }
  }

}
