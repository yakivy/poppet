package poppet.example.tapir.model

import io.circe.Json

case class PoppetRequestInitEvent(
    service: String,
    method: String,
    eagerArguments: Map[String, Json],
    streamArguments: Set[String],
)
