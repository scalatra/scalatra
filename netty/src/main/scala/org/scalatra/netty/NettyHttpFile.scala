package org.scalatra
package netty

import org.jboss.netty.handler.codec.http2.FileUpload
import java.io.{FileInputStream, File}
import org.jboss.netty.buffer.ChannelBufferInputStream

class NettyHttpFile(underlying: FileUpload) extends HttpFile {
  val name = underlying.getFilename

  val contentType = underlying.getContentType

  lazy val size = underlying.length()

  lazy val inputStream = if (underlying.isInMemory) {
    new  ChannelBufferInputStream(underlying.getChannelBuffer)
  } else {
    new FileInputStream(underlying.getFile)
  }

  lazy val bytes = underlying.get()

  lazy val string = underlying.getString

  def saveTo(file: File) = underlying.renameTo(file)

  def delete() = underlying.delete()
}