package poppet.example.tapir.model

case class PoppetRoutingInfo(
    service: String,
    method: String,
    argumentNames: Set[String],
)
