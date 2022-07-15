import org.scalajs.dom.ext.*
import org.scalajs.dom.{document, Element, Fetch, RequestInfo}
import zio.Schedule.spaced
import zio.ZIO.{fromFuture, fromOption, logInfo}
import zio.{durationInt, Task, ZIO, ZIOAppDefault}

import scala.scalajs.js

object Template:
  import scalatags.Text.all.*
  val layout = div(cls := "wrapper")(div(id := "ideas", "Looking for idea,..."))

object DOM:
  val getElementById: String => Task[Element] = elementId =>
    fromOption(Option(document.getElementById(elementId)).filter(_ != null))
      .mapError(_ => new Exception(s"Failed finding DOM element with ID $elementId"))

object ActivityService:
  import scala.scalajs.js.Thenable.Implicits.*

  @js.native
  trait Activity extends js.Object:
    val activity: String = js.native

  private def fetch(info: RequestInfo): Task[js.Any] =
    fromFuture(Fetch.fetch(info).toFuture.flatMap(_.clone().json()))

  def fetchActivity: Task[Activity] =
    fetch("http://www.boredapi.com/api/activity").map(_.asInstanceOf[Activity])

object HelloFe extends ZIOAppDefault:
  val render: Task[Unit] =
    DOM.getElementById("app").map(_.innerHTML += Template.layout.toString)

  def fetchAndRenderActivityIdea: Task[Unit] =
    for
      _        <- logInfo("Fetch new activity,...")
      activity <- ActivityService.fetchActivity
      _        <- DOM.getElementById("ideas").map(_.innerHTML = activity.activity)
    yield ()

  def program: Task[Unit] =
    for
      _ <- render
      _ <- fetchAndRenderActivityIdea.repeat(spaced(10.seconds))
    yield ()

  def run: Task[Unit] = program
