package jp.tattyamm.finagle.sample

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import java.net.InetSocketAddress
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future

/**
 * A somewhat advanced example of using Filters with Clients. Below, HTTP 4xx and 5xx
 * class requests are converted to Exceptions. Additionally, two parallel requests are
 * made and when they both return (the two Futures are joined) the TCP connection(s)
 * are closed.
 */
object HttpClient {

  class InvalidRequest extends Exception

  /**
   * Convert HTTP 4xx and 5xx class responses into Exceptions.
   */
  class HandleErrors extends SimpleFilter[HttpRequest, HttpResponse] {
    def apply(request: HttpRequest, service: Service[HttpRequest, HttpResponse]) = {
      // flatMap asynchronously responds to requests and can "map" them to both
      // success and failure values:
      service(request) flatMap {
        response =>
          response.getStatus match {
            case OK => Future.value(response)
            case FORBIDDEN => Future.exception(new InvalidRequest)
            case _ => Future.exception(new Exception(response.getStatus.getReasonPhrase))
          }
      }
    }
  }

  def main(args: Array[String]) {
    val clientWithoutErrorHandling: Service[HttpRequest, HttpResponse] = ClientBuilder()
      .codec(Http())
      .hosts(new InetSocketAddress(8080))
      .hostConnectionLimit(1)
      .build()

    val handleErrors = new HandleErrors

    // compose the Filter with the client:
    val client: Service[HttpRequest, HttpResponse] = handleErrors andThen clientWithoutErrorHandling

    println("))) Issuing two requests in parallel: ")
    val request1 = makeHelloRequest(client)
    val request2 = makeEchoRequest(client)

    // When both request1 and request2 have completed, close the TCP connection(s).
    (request1 join request2) ensure {
      client.release()
    }
  }

  private[this] def makeHelloRequest(client: Service[HttpRequest, HttpResponse]) = {
    val request = new DefaultHttpRequest(
      HttpVersion.HTTP_1_1, HttpMethod.GET, "/hello")
    //authorizedRequest.addHeader("Authorization", "open sesame")

    client(request) onSuccess {
      response =>
        val responseString = response.getContent.toString(CharsetUtil.UTF_8)
        println("))) Received: " + responseString)
    } onFailure {
      error =>
        println("))) Error   : " + error.getClass.getName)
    }
  }

  private[this] def makeEchoRequest(client: Service[HttpRequest, HttpResponse]) = {
    val request = new DefaultHttpRequest(
      HttpVersion.HTTP_1_1, HttpMethod.GET, "/echo?q=GoodBye!")
    //authorizedRequest.addHeader("Authorization", "open sesame")

    client(request) onSuccess {
      response =>
        val responseString = response.getContent.toString(CharsetUtil.UTF_8)
        println("))) Received: " + responseString)
    } onFailure {
      error =>
        println("))) Error   : " + error.getClass.getName)
    }
  }


}
