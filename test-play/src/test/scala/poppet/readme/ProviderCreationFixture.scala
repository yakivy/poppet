package poppet.readme

import poppet.coder.BiCoder
import scala.concurrent.Future

trait ProviderCreationFixture extends ApiImplementationFixture {
    import poppet.provider.play.all._
    import poppet.coder.play.all._

    val provider = Provider(PlayServer())(ProviderProcessor.generate[UserService](new UserInternalService))
}
