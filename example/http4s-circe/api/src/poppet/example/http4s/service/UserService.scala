package poppet.example.http4s.service

import poppet.example.http4s.model.SR
import poppet.example.http4s.model.User

trait UserService {
    def findById(id: String): SR[User]
}
