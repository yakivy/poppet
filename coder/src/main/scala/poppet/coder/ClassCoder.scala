package poppet.coder

case class ClassCoder[A](name: String, methods: List[MethodCoder[A]])
case class MethodCoder[A](name: String, `return`: TypeCoder[A], arguments: List[TypeCoder[A]])
case class TypeCoder[A](name: String, format: A)