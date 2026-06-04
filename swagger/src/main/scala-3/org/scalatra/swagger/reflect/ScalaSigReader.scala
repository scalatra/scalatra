package org.scalatra.swagger.reflect

private[reflect] object ScalaSigReader {

  def readConstructor(argName: String, clazz: ScalaType, typeArgIndex: Int, argNames: List[String]): Class[?] =
    clazz match {
      case scalaType: ManifestScalaType =>
        org.json4s.reflect.ScalaSigReader.readConstructor(
          argName,
          org.json4s.reflect.ScalaType(
            scalaType.manifest
          ),
          typeArgIndex,
          argNames
        )
    }

  def readConstructor(
      argName: String,
      clazz: ScalaType,
      typeArgIndexes: List[Int],
      argNames: List[String]
  ): Class[?] =
    clazz match {
      case scalaType: ManifestScalaType =>
        org.json4s.reflect.ScalaSigReader.readConstructor(
          argName,
          org.json4s.reflect.ScalaType(
            scalaType.manifest
          ),
          typeArgIndexes,
          argNames
        )
    }

  def readField(name: String, clazz: Class[?], typeArgIndex: Int): Class[?] =
    org.json4s.reflect.ScalaSigReader.readField(name, clazz, typeArgIndex)
}
