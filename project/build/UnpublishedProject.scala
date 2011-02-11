import sbt._

trait UnpublishedProject
  extends BasicManagedProject
{
  override def publishLocalAction = task { None }
  override def deliverLocalAction = task { None }
  override def publishAction = task { None }
  override def deliverAction = task { None }
  override def artifacts = Set.empty
}
