package awsync.utils

import scala.collection.immutable.Seq
import scala.util.{Success, Try}

object Functional {

  /**
   * @return If all tries are success, a success list of those values, if there is fails
   *         the first fail is returned
   */
  def sequence[T](tries: Seq[Try[T]]): Try[Seq[T]] =
    tries.foldLeft[Try[Seq[T]]](Success(Seq())) { (acc, curr) =>
      acc.flatMap(ts => curr.map(t => ts :+ t))
    }
}
