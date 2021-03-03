package poppet.internal

import scala.reflect.macros.blackbox

object Processor {
    def checkSignatures(
        c: blackbox.Context)(tpe: c.Type, methods: Iterable[c.universe.MethodSymbol]
    ): Unit = {
        if (methods.isEmpty) c.abort(c.enclosingPosition,
            s"$tpe has no abstract methods. Make sure that service method parametrized with trait."
        )
        if (methods.map(m => (m.name, m.paramLists.flatten.map(_.name.toString))).toSet.size != methods.size)
            c.abort(c.enclosingPosition, "Use unique argument name lists for overloaded methods.")
    }
}
