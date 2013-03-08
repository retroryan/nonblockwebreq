package controllers

import play.api.mvc._
import java.util.concurrent.TimeUnit

import play.api.libs.concurrent.Promise
import play.Logger

object PausingController extends Controller {

  def pause(duration: Int) = Action {
    Async {
      Logger.info("scala pausing for " + duration*1000)

      Promise.timeout(Ok(duration.toString), duration, TimeUnit.SECONDS)
    }
  }

}
