
Scalatra is an open source project and it would be nice to attract many developers. 

## Reporting issues
If you have a problem but do not have the time or will to dive into the source code, please report it anyway
on [the github issue tracker](https://github.com/scalatra/scalatra/issues), this will allow other developers to keep an eye on it.

If you can create a test case then your issue is likely to be resolved more quickly, and your test can be added to our
suite so it is not recreated later.

When reporting an issue please try to give us all the info you have: scalatra version, operating system, scala version, sbt version,
 jdk version, [phase of the moon](http://www.ist.rit.edu/~jxs/jargon/html/P/phase-of-the-moon.html).


## The Git repository
Whatever you want to do the easiest way is to fork [the Scalatra project on github](https://github.com/scalatra/scalatra/), 
and do your work in your own branch.
As soon as you have something you want the core team to fix, just send a pull request and it should be merged 
quickly into the main repository. 

There is no change too little to be merged in.

### Working with Github

  1. If you are not a github user, check out [their help page](http://help.github.com/) on how to get started, register and go  to step 2
  2. fork us as decribed [here](http://help.github.com/forking/) 
  3. hack hack hack
  4. push to your own forked repository
  5. send a [pull request](https://help.github.com/articles/using-pull-requests)

### Become a committer
Do some good contributions and we'll give you a shiny commit bit!

## Documentation
Good documentation is core to a succesfull and healthy ecosystem, the more we can have, the merrier. 

### Scaladoc
Scaladoc lives inside the scala source code, if you want to contribute scaladoc patches, do the same thing described above:
fork [the github project](https://github.com/scalatra/scalatra/) and write the documentation, then do a pull request. 

The [scaladoc syntax](http://lampsvn.epfl.ch/trac/scala/wiki/Scaladoc/AuthorDocs) is a mix of javadoc and wiki.
Rules for good code documentation are the same as in any other language/framework: why is better than what, compare

    /**
     * A trait representing a Foo
     */
    trait Foo { ... }


and

    /**
     * A Foo is used for bar-ing a Baz
     */
    trait Foo { ... }

### Site documentation / handbook
You can open pull requests for the [Scalatra Documentation](http://scalatra.org/) website in its [GitHub repository]( https://github.com/scalatra/scalatra-website/)

## Code
For the good of the community we should agree on some coding style and conventions, and try to be consistent, but scala itself and scalatra
are young projects, so we are still open to good practices if someone can invent them.

### Style
A few ideas

* Scala is a powerful language, use it's features
* immutability is good
* functional code is good
* types are good, so if you can use a better type do it (e.g. `String` instead of `Any`)
* .. but types are ugly to see: in non-public values try to use type inference when possible
* favor less code and use higher order functions + anonymous functions if you can get away with it
* .. but do not put everything in a function
* traits are better than classes, since they can be composed
* if something uses recursion, use the `@tailrec` annotation to ensure it't tail recursive and will be optimized to a loop
* if some code is not restricted to scalatra usage or http/servlets, put it inside the util package 
* indent with two spaces
* namespace code in org.scalatra
* good methods are short

A document that is followed by many in the Scala community on things such as naming conventions, where to put braces etc is [here](http://davetron5000.github.com/scala-style/ScalaStyleGuide.pdf), it's probably a good idea to follow them.

XXX riffraff: thing I'd do but not sure we currently do: put all data types related to a support inside the same file. 

### Testing
We love testing. When you check in new code it _must_ have tests. If it does not have tests, it's dead code that will be broken in the following days
without anyone noticing. It's ok if you don't code test-first, as long as checked in code ends up with some regression/unit tests.

Of course, you are expected not to break existing code.

Take a look at the existing files in`src/test/` to see examples of how to write test cases.

Basically: test data structures as you would normally do, and test traits destined to be mixed in in ScalatraKernel with the pattern

    // define a servlet mixing in your trait
    class SomethingSupportTestServlet extends ScalatraServlet with SomethingSupport {
      get("/foo") {
        ... use support methods
      }

      post("/bar") {
        ... use support methods
      }
    }

    // define a FunSuite for the servlet as if this was a normal scalatra app
    class FlashMapSupportTest extends ScalatraFunSuite with ShouldMatchers {
      addServlet(classOf[SomethingSupportTestServlet], "/*")

      test("should use the support method sensibly") {
        session {
          post("/bar") {
            header("xxx") should equal(null)
            body should equal("bla bla");
          }

          get("/foo") {
            header("yyy") should equal("posted")
            body should equal("bla bla");
          }

        }
      }
    }

