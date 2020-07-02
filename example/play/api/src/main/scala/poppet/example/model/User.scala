package poppet.example.model

import play.api.libs.json.Json

case class User(email: String, firstName: String)

object User {
    implicit val F = Json.format[User]
}