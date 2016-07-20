package org.sierra

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.sierra.command.Binary
import org.sierra.command.Get
import org.sierra.command.Sets
import scala.util.Try

class PolymorphicEncoder extends FlatSpec with Matchers with RedisSetup {


  it should "hoge" in {
    redis { implicit client =>
      import api._

      val qpath = Path("poly")  :: "morph" :: LongKey("lv") :: Binary(BaseCType)

      val a1 = Sets(ModeA(12, 23))
      val a2 = Sets(ModeB(12, "ab"))

      qpath / 1 <<: a1
      qpath / 2 <<: a2

      qpath / 1 <<: Get() should be(Some(ModeA(12, 23)))
      qpath / 2 <<: Get() should be(Some(ModeB(12, "ab")))

    }
  }

}


sealed trait BaseC {
  def a: Int
}

case class ModeA(a: Int, b: Int) extends BaseC

case class ModeB(a: Int, b: String) extends BaseC

class BaseCType extends ValueType[BaseC] {
  val modeDecoder = """(a|b)@(\d+)@(.+)""".r

  override def decode(coded: Array[Byte]): BaseC = new String(coded) match {
    case modeDecoder("a", aa, bb) if Try(bb.toInt).isSuccess =>
      ModeA(aa.toInt, bb.toInt)
    case modeDecoder("b", xx, yy) =>
      ModeB(xx.toInt, yy)
  }

  override def encode(raw: BaseC): Array[Byte] = raw match {
    case ModeA(aa, bb) =>
      s"a@$aa@$bb".getBytes()
    case ModeB(aa, bb) =>
      s"b@$aa@$bb".getBytes()
  }
}
object BaseCType extends BaseCType
