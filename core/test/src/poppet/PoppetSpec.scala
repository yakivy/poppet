package poppet

import org.scalatest.Assertions
import scala.concurrent.ExecutionContextExecutor

trait PoppetSpec extends Assertions {
    implicit val runNowEc: ExecutionContextExecutor = new ExecutionContextExecutor {
        def execute(runnable: Runnable): Unit = {
            try runnable.run()
            catch { case t: Throwable => reportFailure(t) }
        }
        def reportFailure(t: Throwable): Unit = t.printStackTrace()
    }
}
