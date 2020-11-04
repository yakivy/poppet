package poppet.example.play.module

import play.api.inject.SimpleModule
import play.api.inject._
import poppet.example.play.service.UserService
import poppet.example.play.service.UserServiceProvider

class CustomModule extends SimpleModule(
    bind[UserService].toProvider[UserServiceProvider]
)