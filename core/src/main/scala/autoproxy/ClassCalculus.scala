package autoproxy

import scala.collection.breakOut

trait ClassCalculus { self: MacroBase with TreeSafety =>
  import c.universe._


  def methodsOn(s: Symbol, optSite: Type = NoType): Set[(MethodSymbol,Type)] = {
    val info = optSite match {
      case NoType => s.info
      case site => s infoIn site
    }
//    println(s"info for $s is $info")
    info.members.collect{
      case ms: MethodSymbol =>
        val mssig = ms.typeSignatureIn(info)
//        println(s"$s : $info provides $ms : $mssig")
        ms -> mssig
    }(breakOut)
  }

  def summariseWork(clazzSym: ClassSymbol, pivots: List[Symbol]): WorkSummary =
    new WorkSummaryImpl(clazzSym, pivots)


  private class WorkSummaryImpl(val clazzSym: ClassSymbol, val pivots: List[Symbol]) extends WorkSummary {
    val clazzInfo = clazzSym.info

    val existingMethods: Set[MethodSymbol] = clazzInfo.members.collect{
      case ms: MethodSymbol => ms
    }(breakOut)

    val existingInterfaces = clazzInfo.baseClasses.toSet

    val (existingAbstractMethods, existingConcreteMethods) = existingMethods.partition(_.isAbstract)
    val existingConcreteMethodSigs = existingConcreteMethods map {_.typeSignature}

    val pivotProvidedMethods: Map[Symbol, Set[(MethodSymbol, Type)]] = pivots.map{ p =>
      p -> (methodsOn(p, clazzInfo) filter {case (m,sig) =>
        m.isPublic && !m.isConstructor && !existingConcreteMethodSigs.exists(_ =:= sig)}
      )
    }(breakOut)

    {
      val flatProvidedMethods: Set[(MethodSymbol,Type)] = pivotProvidedMethods.flatMap(_._2)(breakOut)
      val methodsToProviders: Map[(MethodSymbol,Type), Set[Symbol]] = flatProvidedMethods.map{ case (m,sig) =>
        val providers: Set[Symbol] = pivotProvidedMethods.collect{case (k,v) if v contains ((m,sig)) => k}(breakOut)
        (m,sig) -> providers
      }(breakOut)
      val dupes = methodsToProviders.filter(_._2.size > 1)
      //println("dupes: " + dupes)
      for(((method,sig), pivots) <- dupes) {
        c.error(c.enclosingPosition, s"ambiguous proxy, the method '${method.name}${sig}' is provided by ${pivots.mkString("'", "' and '", "'")}")
      }
    }

    val pivotProvidedInterfaces: Map[Symbol, List[Symbol]] = pivots.map{ p =>
      //println(s"pivot base classes for ${p.name} = ${p.info.baseClasses}")
      //println(s"existing base classes for $clazzSym = $existingInterfaces")
      //tail, because we don't want to include the base type itself
      p -> (p.info.baseClasses filterNot existingInterfaces.contains filterNot (_.toString == p.toString))
    }(breakOut)


    private def distinctBy[T,D](xs: List[T])(fn: T => D): List[T] = {
      val b = List.newBuilder[T]
      val seen = collection.mutable.HashSet[D]()
      for (x <- xs) {
        val d = fn(x)
        if (!seen(d)) {
          b += x
          seen += d
        }
      }
      b.result()
    }

    private def stringDistinct[T](xs: List[T]): List[T] = distinctBy(xs)(_.toString)

    val pivotProvidedInterfaceTrees: List[Tree] = stringDistinct(
      for {
        _ <- List(1) // force a List monad
        (_, syms) <- pivotProvidedInterfaces
        sym <- syms
      } yield {
        val symStr = sym.fullName
        //println("sym = " + symStr)
        val symPath = symStr.split('.').toList
        @annotation.tailrec def mkTree(path: List[String], acc: Tree = EmptyTree): Tree = path match {
          case x :: Nil => Select(acc, TypeName(x))
          case h :: t if acc == EmptyTree => mkTree(t, Ident(TermName(h)))
          case h :: t => mkTree(t, Select(acc, TermName(h)))
        }
        val symTree = mkTree(symPath)
        //println(s"$symTree")
        symTree
      }
    )

  }

  trait WorkSummary {
    def clazzSym: ClassSymbol
    def clazzInfo: Type
    def pivots: List[Symbol]

    def existingMethods: Set[MethodSymbol]
    def existingAbstractMethods: Set[MethodSymbol]
    def existingConcreteMethods: Set[MethodSymbol]

    def existingInterfaces: Set[Symbol]

    def pivotProvidedMethods: Map[Symbol, Set[(MethodSymbol, Type)]]

    def pivotProvidedInterfaces: Map[Symbol, List[Symbol]]
    def pivotProvidedInterfaceTrees: List[Tree]
  }


  object TypeSummary {
    def apply(tpe: Type) = new TypeSummary(tpe)

    def fromSymbol(sym: TypeSymbol, site: Type = NoType) = site match {
      case NoType => TypeSummary(sym.info)
      case x      => TypeSummary(sym infoIn x)
    }
  }

  class TypeSummary(val tpe: Type) {
    private def ctorProvidedMemberSymbols : Set[MethodSymbol] = {
      val ctors = tpe.decls.collect{ case ms: MethodSymbol if ms.isConstructor => ms }
      val ctorParams = ctors.flatMap(_.paramLists.flatten)
      ctorParams.collect{
        case ms: MethodSymbol if ms.isVal || ms.isVar => ms
      }(breakOut)
    }

    def methods: Set[MethodSummary] = (ctorProvidedMemberSymbols ++ tpe.members).collect{
      case ms: MethodSymbol if !ms.isConstructor => MethodSummary(ms, TypeSummary(ms typeSignatureIn tpe))
    }
    def abstractMethods: Set[MethodSummary] = methods filter { _.sym.isAbstract }
    def concreteMethods: Set[MethodSummary] = methods filterNot { _.sym.isAbstract }
    def publicConcreteMethods: Set[MethodSummary] = concreteMethods filter { _.sym.isPublic }

    def interfaces: List[ClassSymbol] = tpe.baseClasses.map(_.asClass)
  }
  
  case class MethodSummary(val sym: MethodSymbol, val typeSummary: TypeSummary) {
  }
}
