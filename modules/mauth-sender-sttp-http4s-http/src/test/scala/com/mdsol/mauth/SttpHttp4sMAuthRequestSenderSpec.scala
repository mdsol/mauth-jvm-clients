package com.mdsol.mauth

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.unsafe.implicits.global
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import com.mdsol.mauth.test.utils.TestFixtures
import com.mdsol.mauth.test.utils.TestFixtures._
import com.mdsol.mauth.util.EpochTimeProvider
import com.mdsol.mauth.util.MAuthKeysHelper.getPrivateKeyFromString
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.Inside._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import sttp.client3.basicRequest
import sttp.client3.http4s.Http4sBackend
import sttp.model.{MediaType, Uri}

import java.net.URI
import java.security.Security
import java.util.UUID
import scala.jdk.CollectionConverters._

class SttpHttp4sMAuthRequestSenderSpec extends AsyncWordSpec with BeforeAndAfter with BeforeAndAfterAll {

  Security.addProvider(new BouncyCastleProvider)

  val wiremockServer: WireMockServer = new WireMockServer(wireMockConfig.dynamicPort())

  lazy val requestSender: Resource[IO, SttpHttp4sMAuthRequestSender] = for {
    sttpBackend <- Http4sBackend.usingDefaultEmberClientBuilder[IO]()
  } yield new SttpHttp4sMAuthRequestSender(v1v2Signer, sttpBackend)

  "correctly send auth signatures and content-type header" in {
    val req = basicRequest
      .get(Uri(new URI(s"${wiremockServer.baseUrl()}$REQUEST_NORMALIZE_PATH")))
      .body("")
      .contentType(MediaType.ApplicationJson)

    requestSender
      .use { case sttpSender =>
        sttpSender.send(req).map { _ =>
          inside(getRecordedRequests()) { case List(r) =>
            r.getHeader("content-type") shouldBe "application/json"
            r.getHeader(TIME_HEADER_V1) shouldBe EPOCH_TIME
            r.getHeader(AUTH_HEADER_V1) shouldBe s"MWS $APP_UUID_V2:$SIGNATURE_NORMALIZE_PATH_V1"

            r.getHeader(TIME_HEADER_V2) shouldBe EPOCH_TIME
            r.getHeader(AUTH_HEADER_V2) shouldBe s"MWSV2 $APP_UUID_V2:$SIGNATURE_NORMALIZE_PATH_V2;"
          }
        }
      }
      .unsafeToFuture()
  }

  "sends a default content type (text/plain UTF-8) when content type not specified" in {
    val req = basicRequest.get(Uri(new URI(s"${wiremockServer.baseUrl()}/"))).body("hello")

    requestSender
      .use { case sttpSender =>
        sttpSender.send(req).map { _ =>
          inside(getRecordedRequests()) { case List(r) =>
            r.getHeader("content-type") shouldBe "text/plain; charset=UTF-8"
          }
        }
      }
      .unsafeToFuture()
  }

  private def getRecordedRequests(): List[LoggedRequest] =
    wiremockServer.getAllServeEvents.asScala.toList.map(_.getRequest)

  lazy val v1v2Signer: MAuthSttpSigner = {
    val epochTimeProvider: EpochTimeProvider = () => EPOCH_TIME.toLong
    new MAuthSttpSignerImpl(
      UUID.fromString(APP_UUID_V2),
      getPrivateKeyFromString(TestFixtures.PRIVATE_KEY_2),
      epochTimeProvider,
      SignerConfiguration.ALL_SIGN_VERSIONS
    )
  }

  before {
    wiremockServer.stubFor(
      get(anyUrl())
        .willReturn(aResponse().withStatus(200))
    )
  }

  after {
    wiremockServer.resetAll()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    wiremockServer.start()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    wiremockServer.stop()
  }
}
