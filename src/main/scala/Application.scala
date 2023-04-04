import io.circe.generic.auto._
import io.circe.parser._
import zio._
import zio.http._
import zio.http.model.Method

import java.net.URLEncoder
import scala.io.Source._
case class QuoteData(text: String, author: Option[String])

object Application extends ZIOAppDefault {
  private lazy val QUOTE_JSON = decode[List[QuoteData]](
    fromFile(
      "src/main/resources/quotes.json"
    ).mkString
  )
  private lazy val rand = new util.Random
  val app: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> !! / "quote" =>
      QUOTE_JSON match {
        case Left(_) =>
          Response.redirect(
            "https://img.shields.io/badge/oops-quote%20service%20is%20down-red"
          )
        case Right(quotes) =>
          val quote: QuoteData = quotes(rand.nextInt(quotes.length))
          val text = URLEncoder.encode(quote.text, "UTF-8").replace("+", "%20")
          val author = URLEncoder
            .encode(
              quote.author match {
                case Some(v) => v
                case None    => "anonymous"
              },
              "UTF-8"
            )
            .replace("+", "%20")
          Response.seeOther(
            s"https://img.shields.io/badge/$author-$text-green"
          )
      }
  }
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    Server.serve(app).provide(Server.default)
}
