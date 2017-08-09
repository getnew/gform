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

import java.time.format.DateTimeFormatter

import akka.util.ByteString
import uk.gov.hmrc.gform.fileupload.FileUploadService.FileIds._
import uk.gov.hmrc.gform.sharedmodel.config.ContentType
import uk.gov.hmrc.gform.sharedmodel.form.{ EnvelopeId, FileId }
import uk.gov.hmrc.gform.sharedmodel.formtemplate.{ DmsSubmission, FormTemplateId }
import uk.gov.hmrc.gform.submission.{ SubmissionAndPdf, SubmissionRef }
import uk.gov.hmrc.gform.time.TimeProvider
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FileUploadService(fileUploadConnector: FileUploadConnector, fileUploadFrontendConnector: FileUploadFrontendConnector, timeModule: TimeProvider = new TimeProvider) {

  def createEnvelope(formTypeId: FormTemplateId)(implicit hc: HeaderCarrier): Future[EnvelopeId] = fileUploadConnector.createEnvelope(formTypeId)

  def submitEnvelope(submissionAndPdf: SubmissionAndPdf, dmsSubmission: DmsSubmission)(implicit hc: HeaderCarrier): Future[Unit] = {

    val submissionRef: SubmissionRef = submissionAndPdf.submission.submissionRef
    val envelopeId: EnvelopeId = submissionAndPdf.submission.envelopeId
    val date = timeModule.localDateTime().format(DateTimeFormatter.ofPattern("YYYYMMdd"))
    val fileNamePrefix = s"$submissionRef-$date"
    val reconciliationId = ReconciliationId.create(submissionRef)
    val metadataXml = MetadataXml.xmlDec + "\n" + MetadataXml.getXml(submissionRef, reconciliationId, submissionAndPdf, dmsSubmission)

    val uploadPfdF: Future[Unit] = fileUploadFrontendConnector.upload(
      envelopeId,
      pdf,
      s"$fileNamePrefix-form.pdf",
      ByteString(submissionAndPdf.pdfSummary.pdfContent),
      ContentType.`application/pdf`
    )

    val uploadXmlF: Future[Unit] = fileUploadFrontendConnector.upload(
      envelopeId,
      xml,
      s"$fileNamePrefix-metadata.xml",
      ByteString(metadataXml.getBytes),
      ContentType.`application/xml; charset=UTF-8`
    )

    for {
      _ <- uploadPfdF
      _ <- uploadXmlF
      _ <- fileUploadConnector.routeEnvelope(RouteEnvelopeRequest(envelopeId, "dfs", "DMS"))
    } yield ()
  }
}

object FileUploadService {

  //forbidden keys. make sure they aren't used in templates
  object FileIds {
    val pdf = FileId("pdf")
    val xml = FileId("xmlDocument")
  }
}