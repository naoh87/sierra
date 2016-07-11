package org.sierra

import scodec.Codec
import scodec.bits.BitVector



trait ValueType[A] extends ValueEncoder[A] with ValueDecoder[A]

trait ValueEncoder[-A] {
  def encode(raw: A): Array[Byte]
}

trait ValueDecoder[+A] {
  def decode(coded: Array[Byte]): A
}


object StringValue extends ValueType[String] {
  override def decode(coded: Array[Byte]): String = new String(coded)

  override def encode(raw: String): Array[Byte] = raw.getBytes()
}

object StringInteger extends ValueType[Long] {
  override def decode(coded: Array[Byte]): Long = new String(coded).toLong

  override def encode(raw: Long): Array[Byte] = raw.toString.getBytes()
}

class ScodecEncoder[T](codec: Codec[T]) extends ValueType[T] {
  override def encode(raw: T): Array[Byte] =
    codec.encode(raw).map(_.toByteArray).getOrElse(throw new RuntimeException("unable to encode"))

  override def decode(bits: Array[Byte]) =
    codec.decodeValue(BitVector(bits)).getOrElse(throw new RuntimeException("unable to decode"))

}

object ValueType {
  def apply[T](codec: Codec[T]): ScodecEncoder[T] =
    new ScodecEncoder(codec)
}