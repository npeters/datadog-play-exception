import controllers.ErrorHandlerFilter
import javax.inject.Inject
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSFilter

import scala.concurrent.ExecutionContext

class Filters @Inject() (
    corsFilter: CORSFilter,

    implicit val mat: akka.stream.Materializer,
    implicit val ec: ExecutionContext) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq( corsFilter, ErrorHandlerFilter("Test"))
}
