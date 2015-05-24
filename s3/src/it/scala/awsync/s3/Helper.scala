package awsync.s3

import org.scalatest.Assertions
import org.scalatest.concurrent.AsyncAssertions

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

object Helper {
  implicit class Failing[A](val f: Future[A]) extends Assertions with AsyncAssertions {

    def shouldFailWith[T <: Throwable](implicit m: Manifest[T], ec: ExecutionContext) = {
      val w = new Waiter
      f onComplete {
        case Failure(e) => w(throw e); w.dismiss()
        case Success(_) => w.dismiss()
      }
      intercept[T] {
        w.await
      }
    }
  }
}