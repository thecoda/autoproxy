package autoproxy

trait TreeSafety { self: MacroBase =>
  import c.universe._

  sealed trait TreeSymbol[T <: Tree, S <: Symbol] { def apply(t:T): S }
  implicit object ClassTreeSymbol extends TreeSymbol[ClassDef, ClassSymbol] {
    def apply(t: ClassDef) = t.symbol.asClass
  }

  implicit object ModuleTreeSymbol extends TreeSymbol[ModuleDef, ModuleSymbol] {
    def apply(t: ModuleDef) = t.symbol.asModule
  }

  implicit object DefTreeSymbol extends TreeSymbol[DefDef, MethodSymbol] {
    def apply(t: DefDef) = t.symbol.asMethod
  }

  implicit class SaferTree[T <: Tree](val t: T) {
    def safeDuplicate = t.duplicate.asInstanceOf[T]
    def typechecked = c.typecheck(safeDuplicate, silent = true).asInstanceOf[T]
    def safeSym[S <: Symbol](implicit ts: TreeSymbol[T,S]) = ts(t)
  }


}
