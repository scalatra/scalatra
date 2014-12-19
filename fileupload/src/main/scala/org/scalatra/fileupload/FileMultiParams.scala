package org.scalatra
package fileupload

import org.apache.commons.fileupload.FileItem

/**
 * TODO This is a copy-and-paste abomination that needs to be merged with
 * [[org.scalatra.util.MultiMap]].  We can't properly genericize that this
 * close to a release.
 */
class FileMultiParams(wrapped: Map[String, Seq[FileItem]] = Map.empty) extends Map[String, Seq[FileItem]] {

  def get(key: String): Option[Seq[FileItem]] = {
    (wrapped.get(key) orElse wrapped.get(key + "[]"))
  }

  def get(key: Symbol): Option[Seq[FileItem]] = get(key.name)

  def +[B1 >: Seq[FileItem]](kv: (String, B1)) =
    new FileMultiParams(wrapped + kv.asInstanceOf[(String, Seq[FileItem])])

  def -(key: String) = new FileMultiParams(wrapped - key)

  def iterator = wrapped.iterator

  override def default(a: String): Seq[FileItem] = wrapped.default(a)
}

object FileMultiParams {
  def apply() = new FileMultiParams

  def apply[SeqType <: Seq[FileItem]](wrapped: Map[String, Seq[FileItem]]) =
    new FileMultiParams(wrapped)
}

