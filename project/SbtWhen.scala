object SbtWhen {
  implicit class When[A](a: A) {
    def when(f: A => Boolean)(g: A => A): A = if (f(a)) g(a) else a
  }
}
