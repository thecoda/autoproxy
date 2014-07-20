package autoproxy

import scala.language.experimental.macros
import scala.reflect.internal.util.Position
import scala.reflect.macros.whitebox.Context
import scala.annotation.compileTimeOnly
import collection.breakOut
import LogUtils._

sealed trait InterfaceBehaviour
object withInterfaces extends InterfaceBehaviour
object withoutInterfaces extends InterfaceBehaviour

@compileTimeOnly("`@proxy` must be enclosed in a class annotated as `@delegating`")
class proxy(intf: InterfaceBehaviour = withInterfaces) extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro DelegatingMacro.nakedProxyImpl
}

class delegating extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro DelegatingMacro.impl
}

class summarize extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro DelegatingMacro.summarizeImpl
}

class proxytag extends scala.annotation.StaticAnnotation


object DelegatingMacro {

  def summarizeImpl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    println("enclosing pos: " + c.enclosingPosition)
    //c.error(c.enclosingPosition,"`@summarize` isn't implemented yet")
    val inputs = annottees.map(_.tree).toList
    c.Expr[Any](Block(inputs, Literal(Constant(()))))
  }

  def nakedProxyImpl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    c.abort(c.enclosingPosition,"`@proxy` must be enclosed in a class annotated as `@delegating`")
  }

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    val theMacro = mkInstance(c)

    vprintln("annottees: " + annottees.mkString)

    val inputs = annottees.map(_.tree).toList

    val code = theMacro.process(inputs)

    vprintln(s"delegating macro transform expands to:\n ${code}")

    // Mark range positions for synthetic code as transparent to allow some wiggle room for overlapping ranges
    //for (t <- code) t.setPos(t.pos.makeTransparent)
    c.Expr[Any](Block(code, Literal(Constant(()))))
  }

  def mkInstance(c0: Context): DelegatingMacro { val c: c0.type } = {
    //import language.reflectiveCalls
    new DelegatingMacro {
      //type C = c0.type
      val c: c0.type = c0
    }
  }
}

trait MacroBase {
  //type C <: Context
  val c: Context
}

trait DelegatingMacro extends MacroBase with ClassCalculus with TreeSafety {
  import c.universe._

  def posErr(s: String) = c.error(c.enclosingPosition,s)
  def posWarn(s: String) = c.warning(c.enclosingPosition,s)

  def showDelegateUsageError(): Unit = posErr("The @delegate annotation can only be used on classes or objects")
  def showProxyUsageError(): Unit = posErr("The @proxy annotation can only be used on params, vals, vars or methods")


  def injectIntoTemplate(templ: Template, newMembers: Seq[Tree], newInterfaces: List[Tree]): Template = {
    vprintln(s"templateParents = ${templ.parents}}")
    vprintln(s"injecting interfaces = ${newInterfaces}")
    Template(templ.parents ++ newInterfaces, templ.self, templ.body ++ newMembers)
  }


  def injectIntoImpl[T <: ImplDef](impl: T, newMembers: Seq[Tree], newInterfaces: List[Tree]): T = impl match {
    case ClassDef(mods, name, tparams, templ) =>
      ClassDef(mods, name, tparams, injectIntoTemplate(templ, newMembers, newInterfaces)).asInstanceOf[T]

    case ModuleDef(mods, name, templ) =>
      ModuleDef(mods, name, injectIntoTemplate(templ, newMembers, newInterfaces)).asInstanceOf[T]
  }

  def hasProxyAnnotation(sym: Symbol): Boolean = {
//    println("annotations: " + sym.annotations.mkString(","))
    sym.annotations.exists(_.tree.toString == "new autoproxy.proxytag()")
  }

  def proxyPivots(tpe: TypeSymbol): List[Symbol] = {
    val tpeInfo = tpe.info
    val ctorParams = tpeInfo.decls.collect{
      case ms: MethodSymbol if ms.isConstructor => ms
    }.flatMap(_.paramLists.flatten)

    val candidates = ctorParams ++ tpeInfo.decls
//    val candidateTrees = candidates.map(_.tree)
    vprintln(s"candidate pivots for ${tpe.name} = $candidates")

    val pivots: List[Symbol] = candidates.flatMap{
      case s: Symbol if hasProxyAnnotation(s) => println("identified pivot: " + showRaw(s)); List(s)
      case s: Symbol => vprintln("ignoring symbol " + showRaw(s) + " with " + s.annotations.map(_.tree)); Nil
      case x => vprintln("ignoring non-symbol " + x); Nil
    }(breakOut)

    vprintln(s"pivots for ${tpe.name} = $pivots")
    pivots
  }

  /**
   * @param origins A mapping from pivot symbols to symbols of provided methods
   * @return a List of delgating method trees
   */
  def mkDelegates(origins: Map[Symbol, Set[(MethodSymbol,Type)]]): Seq[Tree] = {
    for {
      (pivot, methods) <- origins.toSeq
      (methodsym, methodtype) <- methods
      if !methodsym.isConstructor
    } yield {
      val name = methodsym.name.toTermName
      val paramss = methodsym.paramLists
      val ret = methodsym.returnType

      val delegateInvocation = {
        val argss = methodsym.paramLists.map( _.map(param => Ident(param.name)) )
        q"${pivot.name.toTermName}.${methodsym.name}(...$argss)"
      }

      val vparamss = methodsym.paramLists.map(_.map {
        paramSymbol => ValDef(
          Modifiers(Flag.PARAM, typeNames.EMPTY, List()),
          paramSymbol.name.toTermName,
          TypeTree(paramSymbol.typeSignature),
          EmptyTree)
      })

      val delegate = q"""def $name(...${vparamss}): $ret = $delegateInvocation"""
      delegate
    }
  }

  object ProxyTaggingTransformer extends Transformer {
    override def transformModifiers(mods: Modifiers): Modifiers = {
      val Modifiers(flags, privateWithin, annotations) = mods
      val updatedannotations = annotations map { ann => ann match {
        case q"new $p()" if p.toString.endsWith("proxy") =>
          q"new autoproxy.proxytag()"
        case x => x
      }}
      Modifiers(flags, privateWithin, updatedannotations)
    }
  }

  def tagProxyAnnotations[T <: Tree](tree: T): T = {
    ProxyTaggingTransformer.transform(tree).asInstanceOf[T]
  }

  def processClass(clazz0: ClassDef): Tree = {
    val clazz = tagProxyAnnotations(clazz0)
    val clazzSym = clazz.typechecked.safeSym
    val pivots = proxyPivots(clazzSym)

    val workSummary = summariseWork(clazzSym, pivots)
    import workSummary.{pivotProvidedMethods, pivotProvidedInterfaceTrees}

    pivotProvidedMethods foreach { case (pivot,methods) =>
      vprintln(s"Provided Methods for ${pivot.name} = ${methods.mkString}")
    }

//    pivotProvidedInterfaces foreach { case (pivot,interfaces) =>
//      vprintln(s"Provided interfaces for ${pivot.name} = ${interfaces.mkString}")
//    }

    if(c.hasErrors) clazz
    else injectIntoImpl(clazz.safeDuplicate, mkDelegates(pivotProvidedMethods), pivotProvidedInterfaceTrees)
  }

  def processModule(mod0: ModuleDef): ModuleDef = {
    val mod = tagProxyAnnotations(mod0)
    val modSym = mod.typechecked.safeSym
    val modClassSym = modSym.moduleClass.asClass
    val pivots = proxyPivots(modClassSym)

    val workSummary = summariseWork(modClassSym, pivots)
    import workSummary.{pivotProvidedMethods, pivotProvidedInterfaceTrees}

    pivotProvidedMethods foreach { case (pivot,methods) =>
      vprintln(s"Provided Methods for ${pivot.name} = ${methods.mkString}")
    }

//    pivotProvidedInterfaces foreach { case (pivot,interfaces) =>
//      vprintln(s"Provided interfaces for ${pivot.name} = ${interfaces.mkString}")
//    }

    val newClassDef = injectIntoImpl(mod.safeDuplicate, mkDelegates(pivotProvidedMethods), pivotProvidedInterfaceTrees)
    newClassDef
  }

  def process(inputs: List[Tree]): List[Tree] = inputs match {
    // Workaround here for https://github.com/scalamacros/paradise/issues/50 when the class lacks a companion
    case (clazz: ClassDef) :: Nil       => processClass(clazz) :: q"object ${clazz.name.toTermName}" :: Nil
    case (clazz: ClassDef) :: rest      => processClass(clazz) :: rest
    case (singleton: ModuleDef) :: rest => processModule(singleton) :: rest
    case _                              => showDelegateUsageError(); inputs
  }
}


