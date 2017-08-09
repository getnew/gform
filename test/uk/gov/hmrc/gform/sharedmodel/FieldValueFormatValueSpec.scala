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

package uk.gov.hmrc.gform.sharedmodel

import play.api.libs.json.{ Reads, _ }
import uk.gov.hmrc.gform.Spec
import uk.gov.hmrc.gform.sharedmodel.formtemplate.{ AnyText, _ }

class FieldValueFormatValueSpec extends Spec {

  "FieldValue json object" should "parse a sum expression" in {
    val fieldValue = toFieldValue(
      """{
           "id": "sum",
           "label": "Label",
           "value": "${amountA + amountB}"
          }
      """
    )
    fieldValue should beJsSuccess(FieldValue(FieldId("sum"), Text(AnyText, Add(FormCtx("amountA"), FormCtx("amountB")), false), "Label", None, None, true, true, true, None))

  }

  // I expect that this charcetr set should be wider, https://confluence.tools.tax.service.gov.uk/display/AF/Field+Expressions
  it should "parse all these characters into a Constant" in {
    val fieldValue = toFieldValue(
      """{
           "id": "constant",
           "label": "Label",
           "value": "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_ ,"
          }
      """
    )
    fieldValue should beJsSuccess(FieldValue(FieldId("constant"), Text(AnyText, Constant("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_ ,"), false), "Label", None, None, true, true, true, None))

  }

  private def toFieldValue(template: String): JsResult[FieldValue] = {

    val templateAsJson = Json.parse(template.stripMargin)

    implicitly[Reads[FieldValue]].reads(templateAsJson)
  }
}