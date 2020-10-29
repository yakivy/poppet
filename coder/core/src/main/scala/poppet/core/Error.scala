package poppet.core

class Error(message: String, e: Throwable) extends Exception(message, e) {
    def this(message: String) = this(message, null)
}
