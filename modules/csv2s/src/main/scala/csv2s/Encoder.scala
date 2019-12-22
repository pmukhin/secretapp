package csv2s

import java.nio.ByteBuffer
import cats.Show

case class Csv(values: List[String])

case class CsvLoad(
  v: List[Csv],
  delim: String = ",",
  lineDelim: String = "\n",
  header: Option[List[String]] = None
) extends Product {

  private def encodeValue(s: String): String =
    if (s.forall(isValidChar) && !s.contains(delim) && !s.contains(lineDelim)) s
    else "\"" + s + "\""

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

  def ofShow2[A, B, C](implicit B: Show[B], C: Show[C]): (A => (B, C)) => Encoder[A] =
    (fun: A => (B, C)) => {
      a: A =>
        val tuple2 = fun(a)
        Csv(List(B.show(tuple2._1), C.show(tuple2._2)))
    }

  def ofShow3[A, B, C, D](
    implicit
    B: Show[B],
    C: Show[C],
    D: Show[D]
  ): (A => (B, C, D)) => Encoder[A] =
    (fun: A => (B, C, D)) => {
      a: A =>
        val tuple3 = fun(a)
        Csv(List(B.show(tuple3._1), C.show(tuple3._2), D.show(tuple3._3)))
    }
  // and so on...
}

object implicits {

  // ops class for list if As
  implicit class ListEncoderOps[A](val a: List[A]) extends AnyVal {

    def asCsv(implicit ev: Encoder[A]): CsvLoad =
      CsvLoad(a.map(ev.asCsv))
  }
}
