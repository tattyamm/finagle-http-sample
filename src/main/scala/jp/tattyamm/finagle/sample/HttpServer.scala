package jp.tattyamm.finagle.sample


import com.twitter.finagle.SimpleFilter
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import java.net.InetSocketAddress
import com.twitter.finagle.builder.Server
import service.{EchoService, HelloService}
import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.http.{RichHttp, Response, Http, Request}
import com.twitter.finagle.Service

object HttpServer extends App {

  /**
   * A simple Filter that catches exceptions and converts them to appropriate
   * HTTP responses.
   */
  class HandleExceptions extends SimpleFilter[Request, Response] {
    def apply(request: Request, service: Service[Request, Response]) = {

      // `handle` asynchronously handles exceptions.
      service(request) handle {
        case error =>
          val statusCode = error match {
            case _: IllegalArgumentException =>
              FORBIDDEN
            case _ =>
              INTERNAL_SERVER_ERROR
          }
          val errorResponse = new DefaultHttpResponse(HTTP_1_1, statusCode)
          errorResponse.setContent(copiedBuffer(error.getStackTraceString, UTF_8))
          Response(errorResponse)
      }
    }
  }

  val handleExceptions = new HandleExceptions

  private def auth(service: Service[Request, Response]) = {
    handleExceptions andThen service
  }

  val routingService =
    RoutingService.byPath {
      case "/hello" => new HelloService
      case "/echo" => new EchoService
    }

  val server: Server = ServerBuilder()
    .codec(RichHttp[Request](Http()))
    .bindTo(new InetSocketAddress(8080))
    .name("httpserver")
    .build(routingService)
}
