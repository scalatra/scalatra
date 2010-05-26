package com.thinkminimo.step

class StepExample extends Step {

  before {
    contentType = "text/html"
  }

  get("/date/:year/:month/:day") {
    <ul>
      <li>Year: {params("year")}</li>
      <li>Month: {params("month")}</li>
      <li>Day: {params("day")}</li>
    </ul>
  }

  get("/form") {
    <form action='post' method='POST'>
      Post something: <input name='submission' type='text'/>
      <input type='submit'/>
    </form>
  }

  post("/post") {
    <h1>You posted: {params("submission")}</h1>
  }

  get("/") {
    <h1>Hello world!</h1>
  }
}
