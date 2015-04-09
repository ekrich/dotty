object assignments {

  var a = Array(1, 2, 3)
  var i = 0
  a(i) = a(i) * 2
  a(i + 1) += 1

  class C {
    var myX = 0
    def x = myX
    def x_=(x: Int) = myX = x

    x = x + 1
    x *= 2

    x_= = 2  // should give missing arguments + reassignment to val
  }

  var c = new C
  import c._ // should give: prefix is not stable
  x = x + 1
  x *= 2
}
