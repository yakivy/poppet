package poppet.core

trait ErrorHandler[A, B] extends (A => B)

trait ModelCoder[A, B] extends (A => B)

trait ExchangeCoder[A, B] extends (A => B)