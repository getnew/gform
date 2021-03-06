/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.gform.Spec
import uk.gov.hmrc.gform.sharedmodel.ExampleData
import uk.gov.hmrc.gform.time.FrozenTimeProvider

class HelperSpec extends Spec {

  "helper.createEnvelopeRequestBody" should "be compatible with fileuplod expectations" in new ExampleData
  with ExampleFileUploadData {
    val helper = new Helper(config, FrozenTimeProvider.exampleInstance)
    helper.createEnvelopeRequestBody(formTemplateId) shouldBe Json.obj(
      "constraints" -> Json.obj(
        "contentTypes" -> Json.arr(
          "application/pdf",
          "application/xml",
          "image/jpeg",
          "text/xml",
          "application/vnd.ms-excel",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ),
        "maxItems"       -> 3,
        "maxSize"        -> "20MB",
        "maxSizePerItem" -> "5MB"
      ),
      "callbackUrl" -> "someCallback",
      "expiryDate"  -> "2017-02-11T05:45:00Z",
      "metadata"    -> Json.obj("application" -> "gform", "formTemplateId" -> "AAA999")
    )
  }

}
