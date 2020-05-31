package poppet.coder

trait Coder[A, B] {
    def apply(a: A): B
}