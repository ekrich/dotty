package dotty.tools.dotc.printing

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.ast.untpd.{PackageDef, Template, TypeDef}
import dotty.tools.dotc.ast.{Trees, untpd}
import dotty.tools.dotc.printing.Texts._
import dotty.tools.dotc.core.Contexts._
import dotty.tools.dotc.core.Flags._
import dotty.tools.dotc.core.Symbols._
import dotty.tools.dotc.core.StdNames._

import scala.language.implicitConversions

class DecompilerPrinter(_ctx: Context) extends RefinedPrinter(_ctx) {

  override protected def filterModTextAnnots(annots: List[untpd.Tree]): List[untpd.Tree] =
    annots.filter(_.tpe != defn.SourceFileAnnotType)

  override protected def blockText[T >: Untyped](trees: List[Trees.Tree[T]]): Text = {
    trees match {
      case DefDef(_, _, _, _, Trees.If(cond, Trees.Block(body :: Nil, _), _)) :: y :: Nil if y.symbol.name == nme.WHILE_PREFIX =>
        keywordText("while") ~ " (" ~ toText(cond) ~ ")" ~ toText(body)
      case DefDef(_, _, _, _, Trees.Block(body :: Nil, Trees.If(cond, _, _))) :: y :: Nil if y.symbol.name == nme.DO_WHILE_PREFIX =>
        keywordText("do") ~ toText(body) ~ keywordText("while") ~ " (" ~ toText(cond) ~ ")"
      case _ => super.blockText(trees.filterNot(_.isInstanceOf[Closure[_]]))
    }
  }

  override protected def packageDefText(tree: PackageDef): Text = {
    val stats = tree.stats.filter {
      case vdef: ValDef[_] => !vdef.symbol.is(Module)
      case _ => true
    }
    val statsText = stats match {
      case (pdef: PackageDef) :: Nil => toText(pdef)
      case _ => toTextGlobal(stats, "\n")
    }
    val bodyText =
      if (tree.pid.symbol.isEmptyPackage) statsText
      else if (currentPrecedence == TopLevelPrec) "\n" ~ statsText
      else " {" ~ statsText ~ "}"
    (keywordStr("package ") ~ toTextPackageId(tree.pid)).provided(!tree.pid.symbol.isEmptyPackage) ~ bodyText
  }

  override protected def templateText(tree: TypeDef, impl: Template): Text = {
    val decl =
      if (!tree.mods.is(Module)) modText(tree.mods, keywordStr(if ((tree).mods is Trait) "trait" else "class"))
      else modText(tree.mods &~ (Final | Module), keywordStr("object"))
    decl ~~ typeText(nameIdText(tree)) ~ withEnclosingDef(tree) { toTextTemplate(impl) } ~ ""
  }

  override protected def toTextTemplate(impl: Template, ofNew: Boolean = false): Text = {
    val Template(constr, parents, self, preBody) = impl
    val impl1 = Template(constr, parents.filterNot(_.symbol.maybeOwner == defn.ObjectClass), self, preBody)
    super.toTextTemplate(impl1, ofNew)
  }

  override protected def defDefToText[T >: Untyped](tree: DefDef[T]): Text = {
    import untpd.{modsDeco => _, _}
    dclTextOr(tree) {
      val printLambda = tree.symbol.isAnonymousFunction
      val prefix = modText(tree.mods, keywordStr("def")) ~~ valDefText(nameIdText(tree)) provided (!printLambda)
      withEnclosingDef(tree) {
        addVparamssText(prefix ~ tparamsText(tree.tparams), tree.vparamss) ~ optAscription(tree.tpt).provided(!printLambda) ~
          optText(tree.rhs)((if (printLambda) " => " else " = ") ~ _)
      }
    }
  }
}
