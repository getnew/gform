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

package uk.gov.hmrc.gform.fileupload

import java.nio.file.{ Files, Paths }

import akka.util.ByteString
import uk.gov.hmrc.gform.sharedmodel.config.ContentType
import uk.gov.hmrc.gform.sharedmodel.form.FileId
import uk.gov.hmrc.gform.sharedmodel.formtemplate.FormTemplateId
import uk.gov.hmrc.gform.time.TimeProvider
import uk.gov.hmrc.gform.wshttp.TestWSHttp
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object DemoApp extends App {

  val config = FUConfig(
    fileUploadBaseUrl = "http://localhost:8898",
    fileUploadFrontendBaseUrl = "http://localhost:8899",
    expiryDays = 30,
    maxSize = "20MB",
    maxSizePerItem = "10MB",
    maxItems = 3,
    contentTypes = List(
      ContentType.`application/pdf`,
      ContentType.`application/xml; charset=UTF-8`,
      ContentType.`image/jpeg`,
      ContentType.`text/plain`,
      ContentType.`application/vnd.ms-excel`,
      ContentType.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
    )
  )

  val http = TestWSHttp

  val timeProvider = new TimeProvider {}
  val fu = new FileUploadConnector(config, http, timeProvider)
  val fuf = new FileUploadFrontendConnector(config, http)
  val fileUploadService = new FileUploadService(fu, fuf)

  val fileBytes = Files.readAllBytes(Paths.get("README.md"))
  val fileBody = ByteString.fromArray(fileBytes)
  implicit val hc = HeaderCarrier()

  val result = for {
  // format: OFF
    envelopeId <- fileUploadService.createEnvelope(FormTemplateId("testFormTypeId"))
    _          <- fuf.upload(envelopeId, FileId("README.md"), "README.md", fileBody, ContentType.`text/plain`)

    x = println(s"envelope created: $envelopeId")
    _ = println(s"file uploaded: $envelopeId")
    // format: ON
  } yield ()

  Await.result(result, Duration.Inf)
}