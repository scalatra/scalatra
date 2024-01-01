object SbtWhen {
  implicit class When[A](a: A) {
    def whenRunningOnJvm17OrAbove(f: A => A): A = {
      if (sys.props("java.specification.version").toInt >= 17) f(a) else a
    }
  }
}
