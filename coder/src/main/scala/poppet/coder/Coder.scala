package poppet.coder

case class BiCoder[A, B, F[_]](
    forward: Coder[A, F[B]],
    backward: Coder[B, F[A]]
)

trait Coder[A, B] {
    def apply(a: A): B
}