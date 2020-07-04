package poppet.dto

case class Request[I](service: String, method: String, arguments: Map[String, I])
