package poppet.coder.instances

import poppet.coder.Coder

trait CoderInstances {
    implicit def idCoder[A]: Coder[A, A] = identity(_)
}
