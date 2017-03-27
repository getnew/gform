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

package uk.gov.hmrc.bforms.core

import uk.gov.hmrc.bforms.exceptions.InvalidState
import uk.gov.hmrc.bforms.models.{ FieldId, FormField, Section }

object TemplateValidator {

  def getMatchingSection(formFields: Seq[FormField], sections: Seq[Section]): Opt[Section] = {
    val formFieldIds: Set[FieldId] = formFields.map(_.id).toSet
    val sectionOpt: Option[Section] = sections.find { section =>
      val sectionIds: Set[FieldId] = section.fields.flatMap { field =>
        field.`type` match {
          case Some(Address) => Address.fields(field.id)
          case Some(Date) => Date.fields(field.id)
          case otherwise => List(field.id)
        }
      }.toSet
      sectionIds == formFieldIds
    }

    sectionOpt match {
      case Some(section) => Right(section)
      case None =>
        val sectionsForPrint = sections.map(_.fields.map(_.id))

        Left(InvalidState(s"""|Cannot find a section corresponding to the formFields
                              |FormFields: $formFieldIds
                              |Sections: $sectionsForPrint""".stripMargin))
    }
  }
}
