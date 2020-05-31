package service

import poppet.example.service.HelloService

class HelloInternalService extends HelloService {
    override def apply(): String = "Hello from service!"
}
