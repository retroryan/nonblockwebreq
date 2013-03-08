package controllers

import play.api.mvc._
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Promise
import play.Logger

object PausingController extends Controller {

  def pause(duration: Int) = Action {
    Async {
      Logger.debug("scala pausing for " + duration)

      Promise.timeout(Ok(duration.toString), duration*1000)
    }
  }

}
