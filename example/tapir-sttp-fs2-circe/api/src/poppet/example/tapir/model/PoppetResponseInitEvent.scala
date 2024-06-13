package poppet.example.tapir.model

import io.circe.Json

case class PoppetResponseInitEvent(
    eagerResponse: Option[Json],
)
