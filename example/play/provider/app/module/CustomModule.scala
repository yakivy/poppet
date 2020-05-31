package module

import play.api.inject.SimpleModule
import play.api.inject._
import poppet.example.service.HelloService
import service.HelloInternalService

class CustomModule extends SimpleModule(
    bind[HelloService].to[HelloInternalService]
)
