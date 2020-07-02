package module

import play.api.inject.SimpleModule
import play.api.inject._
import poppet.example.service.UserService
import service.UserInternalService

class CustomModule extends SimpleModule(
    bind[UserService].to[UserInternalService]
)
