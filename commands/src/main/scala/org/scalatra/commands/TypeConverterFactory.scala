package org.scalatra
package commands

import org.scalatra.util.conversion._

trait TypeConverterFactory[T] extends BindingImplicits {

  def resolveMultiParams: TypeConverter[Seq[String], T]
  def resolveStringParams: TypeConverter[String, T]
}

trait TypeConverterFactories extends BindingImplicits {
  implicit def typeConverterFactory[A](implicit seqConverter: TypeConverter[Seq[String], A], stringConverter: TypeConverter[String, A]): TypeConverterFactory[A] =
    new TypeConverterFactory[A] {
      def resolveMultiParams: TypeConverter[Seq[String], A] = seqConverter

      def resolveStringParams: TypeConverter[String, A] = stringConverter
    }
}
object TypeConverterFactories extends TypeConverterFactories
