package org.silkframework.rule.expressions

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.rule.input.Input
import org.silkframework.rule.plugins.transformer.numeric.{AggregateNumbersTransformer, LogarithmTransformer}
import org.silkframework.rule.plugins.transformer.replace.ReplaceTransformer

/**
  * Defines all test, which are executed for both the ExpressionParser and the ExpressionWriter.
  */
abstract class ExpressionTestBase extends FlatSpec with Matchers {

  val generator = new ExpressionGenerator
  import generator._

  implicit val prefixes = Prefixes(Map("f" -> "http://example.org/prefix"))

  it should "parse constants" in {
    check(
      expr = "3.14",
      tree = constant("3.14")
    )
  }

  it should "parse variables" in {
    check(
      expr = "x",
      tree = path("x")
    )
    check(
      expr = "f:weight",
      tree = path("f:weight")
    )
  }

  it should "parse multiplications between two variables" in {
    check(
      expr = "f:height * f:weight",
      tree = numOp(path("f:height"), "*", path("f:weight"))
    )
  }

  it should "parse multiplications between three variables" in {
    check(
      expr = "a * b * c",
      tree = numOp(numOp(path("a"), "*", path("b")), "*", path("c"))
    )
  }

  it should "parse additions between three variables" in {
    check(
      expr = "a + b + c",
      tree = numOp(numOp(path("a"), "+", path("b")), "+", path("c"))
    )
    check(
      expr = "a + b - c",
      tree = numOp(numOp(path("a"), "+", path("b")), "-", path("c"))
    )
  }

  it should "parse multiplications before additions" in {
    check(
      expr = "a + b * c",
      tree = numOp(path("a"), "+", numOp(path("b"), "*", path("c")))
    )
    check(
      expr = "a - b / c",
      tree = numOp(path("a"), "-", numOp(path("b"), "/", path("c")))
    )
  }

  it should "parse multiplications with a constant" in {
    check(
      expr = "a * 1.0",
      tree = numOp(path("a"), "*", constant("1.0"))
    )
  }

  it should "parse function invocations" in {
    check(
      expr = "log(x)",
      tree = func(LogarithmTransformer(), path("x"))
    )
    check(
      expr = "log(1.0)",
      tree = func(LogarithmTransformer(), constant("1.0"))
    )
  }

  it should "parse function invocations with expressions inside" in {
    check(
      expr = "log(a * b)",
      tree = func(LogarithmTransformer(), numOp(path("a"), "*", path("b")))
    )
  }

  it should "parse expressions with function invocations" in {
    check(
      expr = "3.0 + log(x) * y",
      tree = numOp(constant("3.0"), "+", numOp(func(LogarithmTransformer(), path("x")), "*", path("y")))
    )
  }

  it should "parse function invocations with a single parameter" in {
    check(
      expr = "log[base=16](x)",
      tree = func(LogarithmTransformer(base = 16), path("x"))
    )
  }

  it should "parse function invocations with multiple parameters" in {
    check(
      expr = "replace[search=x;replace=y](x)",
      tree = func(ReplaceTransformer(search = "x", replace = "y"), path("x"))
    )
  }

  it should "parse function invocations with empty parameters" in {
    check(
      expr = "replace[search=toRemove;replace=](x)",
      tree = func(ReplaceTransformer(search = "toRemove", replace = ""), path("x"))
    )
  }

  it should "parse function invocations with escaped parameter values" in {
    check(
      expr = "replace[search=yyy\\;xxx;replace=xxx\\]yyy](x)",
      tree = func(ReplaceTransformer(search = "yyy;xxx", replace = "xxx]yyy"), path("x"))
    )
  }

  it should "parse function invocations with multiple variables" in {
    check(
      expr = "aggregateNumbers[operator=max](5;3)",
      tree = func(AggregateNumbersTransformer(operator = "max"), Seq(constant("5"), constant("3")))
    )
    check(
      expr = "aggregateNumbers[operator=max](Some+Path;Some+Other+Path)",
      tree = func(AggregateNumbersTransformer(operator = "max"), Seq(path("Some+Path"), path("Some+Other+Path")))
    )
  }

  it should "parse nested function invocations" in {
    check(
      expr = "aggregateNumbers[operator=max](log[base=16](5);3)",
      tree = func(AggregateNumbersTransformer(operator = "max"), Seq(func(LogarithmTransformer(base = 16), constant("5")), constant("3")))
    )
  }

  protected def check(expr: String, tree: Input): Unit

}