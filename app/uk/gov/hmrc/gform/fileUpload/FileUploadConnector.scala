/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.gform.fileUpload

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.http.HeaderNames.LOCATION
import play.api.libs.json.Json
import uk.gov.hmrc.gform.config.ConfigModule
import uk.gov.hmrc.gform.models.FormTypeId
import uk.gov.hmrc.gform.time.TimeModule
import uk.gov.hmrc.gform.wshttp.WSHttpModule
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{ HeaderCarrier, HttpResponse }

import scala.concurrent.{ ExecutionContext, Future }

class FileUploadModule(configModule: ConfigModule, wSHttpModule: WSHttpModule, timeModule: TimeModule) {

  lazy val fileUploadConnector: FileUploadConnector = new FileUploadConnector(config, wSHttpModule.auditableWSHttp, timeModule.localDateTime())

  private lazy val config: Config = Config(
    configModule.serviceConfig.baseUrl("file-upload"),
    ac.formExpiryDays,
    s"${ac.formMaxAttachments * ac.formMaxAttachmentSizeMB + 10}MB", //heuristic to compute max size
    s"${ac.formMaxAttachmentSizeMB}MB",
    ac.formMaxAttachments
  )

  //TODO: provide separate one here
  private lazy implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  private lazy val ac = configModule.appConfig
}

class FileUploadConnector(config: Config, wSHttp: WSHttp, now: => LocalDateTime)(implicit ec: ExecutionContext) {

  def createEnvelope(formTypeId: FormTypeId)(implicit hc: HeaderCarrier): Future[EnvelopeId] =
    wSHttp
      .POST(s"$baseUrl/file-upload/envelopes", createEnvelopeIn(formTypeId))
      .map(extractEnvelopId)

  /**
   * There must be Location header. If not this is exceptional situation!
   */
  private def extractEnvelopId(resp: HttpResponse): EnvelopeId = resp.header(LOCATION) match {
    case Some(EnvelopeIdExtractor(envelopeId)) => EnvelopeId(envelopeId)
    case Some(location) => throw new SpoiltLocationHeader(location)
    case _ => throw new SpoiltLocationHeader(s"Header $LOCATION not found")
  }

  private lazy val EnvelopeIdExtractor = "envelopes/([\\w\\d-]+)$".r.unanchored
  private val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  private def envelopeExpiryDate = now.plusDays(config.expiryDays).format(formatter)

  private def createEnvelopeIn(formTypeId: FormTypeId) = Json.obj(
    "constraints" -> Json.obj(
      "contentTypes" -> Json.arr(
        "application/pdf",
        "image/jpeg"
      ),
      "maxItems" -> config.maxItems,
      "masSize" -> config.maxSize,
      "maxSizePerItem" -> config.maxSizePerItem
    ),
    "callbackUrl" -> "someCallback", //TODO
    "expiryDate" -> s"$envelopeExpiryDate",
    "metadata" -> Json.obj(
      "application" -> "gform",
      "formTypeId" -> s"${formTypeId.value}"
    )
  )

  private lazy val baseUrl = config.baseUrl
}

case class Config(
  baseUrl: String,
  expiryDays: Long,
  maxSize: String,
  maxSizePerItem: String,
  maxItems: Int
)

class SpoiltLocationHeader(val message: String) extends RuntimeException(message)

case class EnvelopeId(value: String)
