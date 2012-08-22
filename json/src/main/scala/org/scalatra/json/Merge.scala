package org.scalatra
package json

/*
 * Copyright 2009-2010 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import AST._
/** Use fundep encoding to improve return type of merge function
 *  (see: http://www.chuusai.com/2011/07/16/fundeps-in-scala/)
 *
 *  JObject merge JObject = JObject
 *  JArray  merge JArray  = JArray
 *  _       merge _       = JValue
 */
private[json] trait MergeDep[A <: JValue, B <: JValue, R <: JValue] {
  def apply(val1: A, val2: B): R
}

private[json] trait LowPriorityMergeDep {
  implicit def jjj[A <: JValue, B <: JValue] = new MergeDep[A, B, JValue] {
    def apply(val1: A, val2: B): JValue = merge(val1, val2)

    private def merge(val1: JValue, val2: JValue): JValue = (val1, val2) match {
      case (JObject(xs), JObject(ys)) => JObject(Merge.mergeFields(xs, ys))
      case (JArray(xs), JArray(ys)) => JArray(Merge.mergeVals(xs, ys))
      case (JField(n1, v1), JField(n2, v2)) if n1 == n2 => JField(n1, merge(v1, v2))
      case (f1: JField, f2: JField) => f2
      case (JNothing, x) => x
      case (x, JNothing) => x
      case (_, y) => y
    }
  }
}

private[json] trait MergeDeps extends LowPriorityMergeDep {
  implicit object ooo extends MergeDep[JObject, JObject, JObject] {
    def apply(val1: JObject, val2: JObject): JObject = JObject(Merge.mergeFields(val1.fields, val2.fields))
  }

  implicit object aaa extends MergeDep[JArray, JArray, JArray] {
    def apply(val1: JArray, val2: JArray): JArray = JArray(Merge.mergeVals(val1.elements, val2.elements))
  }
}

/** Function to merge two JSONs.
 */
object Merge {
  /** Return merged JSON.
   * <p>
   * Example:<pre>
   * val m = ("name", "joe") ~ ("age", 10) merge ("name", "joe") ~ ("iq", 105)
   * m: JObject(List(JField(name,JString(joe)), JField(age,JInt(10)), JField(iq,JInt(105))))
   * </pre>
   */
  def merge[A <: JValue, B <: JValue, R <: JValue]
    (val1: A, val2: B)(implicit instance: MergeDep[A, B, R]): R = instance(val1, val2)

  private[json] def mergeFields(vs1: List[JField], vs2: List[JField]): List[JField] = {
    def mergeRec(xleft: List[JField], yleft: List[JField]): List[JField] = xleft match {
      case Nil => yleft
      case JField(xn, xv) :: xs => yleft find (_.name == xn) match {
        case Some(y @ JField(yn, yv)) =>
          JField(xn, merge(xv, yv)) :: mergeRec(xs, yleft filterNot (_ == y))
        case None => JField(xn, xv) :: mergeRec(xs, yleft)
      }
    }

    mergeRec(vs1, vs2)
  }

  private[json] def mergeVals(vs1: List[JValue], vs2: List[JValue]): List[JValue] = {
    def mergeRec(xleft: List[JValue], yleft: List[JValue]): List[JValue] = xleft match {
      case Nil => yleft
      case x :: xs => yleft find (_ == x) match {
        case Some(y) => merge(x, y) :: mergeRec(xs, yleft filterNot (_ == y))
        case None => x :: mergeRec(xs, yleft)
      }
    }

    mergeRec(vs1, vs2)
  }

  private[json] trait Mergeable extends MergeDeps {
    implicit def j2m[A <: JValue](json: A): MergeSyntax[A] = new MergeSyntax(json)

    class MergeSyntax[A <: JValue](json: A) {
      /** Return merged JSON.
       * @see com.codahale.jerkson.Merge#merge
       */
      def merge[B <: JValue, R <: JValue](other: B)(implicit instance: MergeDep[A, B, R]): R =
        Merge.merge(json, other)(instance)
    }
  }
}