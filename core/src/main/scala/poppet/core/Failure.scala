package poppet.core

class Failure(message: String, e: Throwable) extends Exception(message, e) {
    def this(message: String) = this(message, null)
}

class DecodingFailure[I](message: String, data: I, e: Throwable) extends Failure(message, e) {
    def this(message: String, data: I) = this(message, data, null)
}
