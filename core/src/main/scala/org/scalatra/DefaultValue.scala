//package org.scalatra
//
//import collection.generic.CanBuildFrom
//
///*
// * All credit for the code in this file is a minimized version of scalaz' Zero
// * in scalaz 7 the Zero type class has been removed and zero's only exist on a monoid.
// * Because we were abusing the Zero typeclass as a way to provide default values this
// * retains that functionality without the abuse of the identity value 
// */
//
///**
// * A DefaultValue in type Z provides a default value for a given type Z
// */
//trait DefaultValue[Z] {
//  val default: Z
//}
//
//trait DefaultValues {
//  def default[Z](z: Z): DefaultValue[Z] = new DefaultValue[Z] {
//    val default = z
//  }
//
//  def mdefault[Z](implicit z: DefaultValue[Z]): Z = z.default
//}
//object DefaultValues extends DefaultValues
//
//object DefaultValue {
//  import xml.{ Elem, Node, NodeSeq }
//
//  implicit def UnitDefaultValue: DefaultValue[Unit] = default(())
//
//  implicit def StringDefaultValue: DefaultValue[String] = default("")
//
//  implicit def IntDefaultValue: DefaultValue[Int] = default(0)
//
//  implicit def BooleanDefaultValue: DefaultValue[Boolean] = default(false)
//
//  implicit def CharDefaultValue: DefaultValue[Char] = default(0.toChar)
//
//  implicit def ByteDefaultValue: DefaultValue[Byte] = default(0.toByte)
//
//  implicit def LongDefaultValue: DefaultValue[Long] = default(0L)
//
//  implicit def ShortDefaultValue: DefaultValue[Short] = default(0.toShort)
//
//  implicit def FloatDefaultValue: DefaultValue[Float] = default(0F)
//
//  implicit def DoubleDefaultValue: DefaultValue[Double] = default(0D)
//
//  implicit def BigIntegerDefaultValue = default(java.math.BigInteger.valueOf(0))
//
//  implicit def BigIntDefaultValue: DefaultValue[BigInt] = default(BigInt(0))
//
//  implicit def TraversableDefaultValue[CC <: Traversable[_]](implicit cbf: CanBuildFrom[Nothing, Nothing, CC]): DefaultValue[CC] =
//    default(cbf.apply.result)
//
//  // Not implicit to ensure implicitly[DefaultValue[NodeSeq]].default === NodeSeqDefaultValue.default
//  def NodeDefaultValue: DefaultValue[Node] = new DefaultValue[Node] {
//    val default = new Node {
//      override def text = null
//
//      override def label = null
//
//      override def child = Nil
//    }
//  }
//
//  // Not implicit to ensure implicitly[DefaultValue[NodeSeq]].default === NodeSeqDefaultValue.default
//  def ElemDefaultValue: DefaultValue[Elem] = new DefaultValue[Elem] {
//    val default = new Elem(null, null, scala.xml.Null, xml.TopScope, Nil: _*)
//  }
//
//  implicit def OptionDefaultValue[A]: DefaultValue[Option[A]] = default(None)
//
//  implicit def ArrayDefaultValue[A: Manifest]: DefaultValue[Array[A]] = default(new Array[A](0))
//
//  implicit def EitherLeftDefaultValue[A, B](implicit bz: DefaultValue[B]): DefaultValue[Either.LeftProjection[A, B]] = default(Right(mdefault[B]).left)
//
//  implicit def EitherRightDefaultValue[A: DefaultValue, B]: DefaultValue[Either.RightProjection[A, B]] = default(Left(mdefault[A]).right)
//
//  implicit def EitherDefaultValue[A: DefaultValue, B]: DefaultValue[Either[A, B]] = default(Left(mdefault[A]))
//
//  implicit def MapDefaultValue[K, V: DefaultValue]: DefaultValue[Map[K, V]] = default(Map.empty[K, V])
//
//  implicit def Tuple2DefaultValue[A, B](implicit az: DefaultValue[A], bz: DefaultValue[B]): DefaultValue[(A, B)] =
//    default((az.default, bz.default))
//
//  implicit def Tuple3DefaultValue[A, B, C](implicit az: DefaultValue[A], bz: DefaultValue[B], cz: DefaultValue[C]): DefaultValue[(A, B, C)] =
//    default((az.default, bz.default, cz.default))
//
//  implicit def Tuple4DefaultValue[A, B, C, D](implicit az: DefaultValue[A], bz: DefaultValue[B], cz: DefaultValue[C], dz: DefaultValue[D]): DefaultValue[(A, B, C, D)] =
//    default((az.default, bz.default, cz.default, dz.default))
//
//  implicit def Function1ABDefaultValue[A, B: DefaultValue]: DefaultValue[A ⇒ B] = default((_: A) ⇒ mdefault[B])
//
//  import java.util._
//  import java.util.concurrent._
//
//  implicit def JavaArrayListDefaultValue[A]: DefaultValue[ArrayList[A]] = default(new ArrayList[A])
//
//  implicit def JavaHashMapDefaultValue[K, V]: DefaultValue[HashMap[K, V]] = default(new HashMap[K, V])
//
//  implicit def JavaHashSetDefaultValue[A]: DefaultValue[HashSet[A]] = default(new HashSet[A])
//
//  implicit def JavaHashtableDefaultValue[K, V]: DefaultValue[Hashtable[K, V]] = default(new Hashtable[K, V])
//
//  implicit def JavaIdentityHashMapDefaultValue[K, V] = default(new IdentityHashMap[K, V])
//
//  implicit def JavaLinkedHashMapDefaultValue[K, V]: DefaultValue[LinkedHashMap[K, V]] = default(new LinkedHashMap[K, V])
//
//  implicit def JavaLinkedHashSetDefaultValue[A]: DefaultValue[LinkedHashSet[A]] = default(new LinkedHashSet[A])
//
//  implicit def JavaLinkedListDefaultValue[A]: DefaultValue[LinkedList[A]] = default(new LinkedList[A])
//
//  implicit def JavaPriorityQueueDefaultValue[A]: DefaultValue[PriorityQueue[A]] = default(new PriorityQueue[A])
//
//  implicit def JavaStackDefaultValue[A]: DefaultValue[Stack[A]] = default(new Stack[A])
//
//  implicit def JavaTreeMapDefaultValue[K, V]: DefaultValue[TreeMap[K, V]] = default(new TreeMap[K, V])
//
//  implicit def JavaTreeSetDefaultValue[A]: DefaultValue[TreeSet[A]] = default(new TreeSet[A])
//
//  implicit def JavaVectorDefaultValue[A]: DefaultValue[Vector[A]] = default(new Vector[A])
//
//  implicit def JavaWeakHashMapDefaultValue[K, V]: DefaultValue[WeakHashMap[K, V]] = default(new WeakHashMap[K, V])
//
//  implicit def JavaArrayBlockingQueueDefaultValue[A]: DefaultValue[ArrayBlockingQueue[A]] = default(new ArrayBlockingQueue[A](0))
//
//  implicit def JavaConcurrentHashMapDefaultValue[K, V]: DefaultValue[ConcurrentHashMap[K, V]] = default(new ConcurrentHashMap[K, V])
//
//  implicit def JavaConcurrentLinkedQueueDefaultValue[A]: DefaultValue[ConcurrentLinkedQueue[A]] = default(new ConcurrentLinkedQueue[A])
//
//  implicit def JavaCopyOnWriteArrayListDefaultValue[A]: DefaultValue[CopyOnWriteArrayList[A]] = default(new CopyOnWriteArrayList[A])
//
//  implicit def JavaCopyOnWriteArraySetDefaultValue[A]: DefaultValue[CopyOnWriteArraySet[A]] = default(new CopyOnWriteArraySet[A])
//
//  implicit def JavaLinkedBlockingQueueDefaultValue[A]: DefaultValue[LinkedBlockingQueue[A]] = default(new LinkedBlockingQueue[A])
//
//  implicit def JavaSynchronousQueueDefaultValue[A]: DefaultValue[SynchronousQueue[A]] = default(new SynchronousQueue[A])
////}