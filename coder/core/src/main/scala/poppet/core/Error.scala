package poppet.core

class Error(message: String, e: Throwable) extends Exception(s"[Poppet] $message", e) {
    def this(message: String) = this(message, null)
}
