package poppet.readme

import org.scalatest.WordSpec

class PlayProviderQuickStartSpec extends WordSpec with ProviderCreationFixture {
    "Provider materialization" should {
        "return valid action" in {
            val action = provider.materialize()

            assert(true)
        }
    }
}
