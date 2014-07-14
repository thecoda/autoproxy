package sandbox

import scala.reflect.macros.whitebox.Context


trait AnnotationMacro {
  type C <: Context
  val c: C
  import c.universe._

  def processClass(clazz: ClassDef, optMod: Option[ModuleDef] = None): (ClassDef, Option[ModuleDef])
  def processModule(mod: ModuleDef): ModuleDef
  def processParam(param: Tree, owner: Tree, companion: Option[ImplDef]): ModuleDef
}
