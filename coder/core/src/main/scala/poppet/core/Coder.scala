package poppet.core

trait Coder[A, B] {
    def apply(a: A): B
    def compose[C](coder: Coder[C, A]): Coder[C, B] = c => apply(coder(c))
    def andThen[C](coder: Coder[B, C]): Coder[A, C] = a => coder(apply(a))
}

trait ExchangeCoder[A, B] extends Coder[A, B]