package poppet.coder

trait Coder[A, B] {
    def apply(a: A): B
    def compose[C](coder: Coder[C, A]): Coder[C, B] = c => apply(coder(c))
}