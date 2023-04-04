import io.circe.generic.auto._
import io.circe.parser._
import zio._
import zio.http._
import zio.http.model.Method

import scala.io.Source._
case class QuoteData(text: String, author: Option[String])

object Application extends ZIOAppDefault {
  private lazy val QUOTE_JSON = decode[List[QuoteData]](
    fromFile(
      "src/main/resources/quotes.json"
    ).mkString
  )
  private lazy val rand = new util.Random
  private val app: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> _ / "quote" =>
      val (author, text, color) = QUOTE_JSON match {
        case Right(quotes) =>
          val qd: QuoteData = quotes(rand.nextInt(quotes.length))
          (qd.author.getOrElse("anonymous"), qd.text, "green")
        case _ => ("oops", "Quote Service is Down", "red")
      }
      Response.redirect(s"https://img.shields.io/badge/$author-$text-$color")
  }
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    Server.serve(app).provide(Server.default)
}
