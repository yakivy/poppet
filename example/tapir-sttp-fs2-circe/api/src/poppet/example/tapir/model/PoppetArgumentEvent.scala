package poppet.example.tapir.model

import io.circe.Json

case class PoppetArgumentEvent(argumentName: String, values: List[Json])
