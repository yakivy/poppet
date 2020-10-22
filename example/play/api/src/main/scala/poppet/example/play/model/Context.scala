package poppet.example.play.model

import play.api.libs.json.Json

case class Context(authSecret: String)

object Context {
    implicit val F = Json.format[Context]
}