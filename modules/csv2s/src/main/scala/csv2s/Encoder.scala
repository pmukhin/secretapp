package csv2s

import java.nio.ByteBuffer

case class Csv(values: List[String])

case class CsvLoad(
  v: List[Csv],
  delim: String = ",",
  lineDelim: String = "\n",
  header: Option[List[String]] = None
) {

  private def encodeValue(s: String): String =
    if (s.forall(isValidChar) && !s.contains(delim)) s else "\"" + s + "\""

  private def isValidChar(c: Char): Boolean =
    c != '"'

  private def encodeHeader =
    header.map(_.mkString(delim) + lineDelim).getOrElse("")

  def stringify: String =
    encodeHeader + v.map(_.values.map(encodeValue).mkString(delim)).mkString(lineDelim)

  def toBytes: Array[Byte]  = stringify.getBytes
  def toByteBuf: ByteBuffer = ByteBuffer.wrap(toBytes)
}

// typeclass for encoding objects
trait Encoder[A] {
  def asCsv(a: A): Csv
}

object Encoder {

  // a little hacky but works for 99% of cases
  // (x: String).toString == x
  def simple[A]: (A => List[Any]) => Encoder[A] =
    (fun: A => List[Any]) => (a: A) => Csv(fun(a).map(_.toString))
}

object implicits {

  implicit class ListEncoderOps[A](val a: List[A]) extends AnyVal {

    def asCsv(implicit ev: Encoder[A]): CsvLoad =
      CsvLoad(a.map(ev.asCsv))
  }
}
