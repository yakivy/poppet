package poppet.core

case class Request[I](
    service: String, method: String, arguments: Map[String, I]
)
