package org.scalatra.fileupload

class FileMultiParamsServlet3(wrapped: Map[String, Seq[FileItemServlet3]] = Map.empty) extends Map[String, Seq[FileItemServlet3]] {

  def get(key: String): Option[Seq[FileItemServlet3]] = {
    (wrapped.get(key) orElse wrapped.get(key + "[]"))
  }

  def get(key: Symbol): Option[Seq[FileItemServlet3]] = get(key.name)

  def +[B1 >: Seq[FileItemServlet3]](kv: (String, B1)) =
    new FileMultiParamsServlet3(wrapped + kv.asInstanceOf[(String, Seq[FileItemServlet3])])

  def -(key: String) = new FileMultiParamsServlet3(wrapped - key)

  def iterator = wrapped.iterator

  override def default(a: String): Seq[FileItemServlet3] = wrapped.default(a)
}

object FileMultiParamsServlet3 {
  def apply() = new FileMultiParamsServlet3

  def apply[SeqType <: Seq[FileItemServlet3]](wrapped: Map[String, Seq[FileItemServlet3]]) =
    new FileMultiParamsServlet3(wrapped)
}