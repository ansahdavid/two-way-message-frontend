/*
 * Copyright 2019 HM Revenue & Customs
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

package models

import org.scalatest._
import play.api.libs.json.{Json, _}
import models.MessageFormat._
import assets.Fixtures

class MessageFormatSpec extends WordSpec with Fixtures with Matchers {

  "Message json reader" should {
    "read v3 message as defined in message microservice " in {
      val id = "123456"
      val json = Json.parse(v3Message(s"${id}"))
      val messageResult = json.validate[MessageV3]
      messageResult shouldBe a[JsSuccess[_]]
      messageResult.get shouldBe a[MessageV3]
      messageResult.get.validFrom.toString should be("2013-12-01")
    }

    "read v3 messages as defined in message microservice " in {
      val id1 = "123456"
      val id2 = "654321"
      val json = Json.parse(v3Messages(id1, id2))
      val messageResult = json.validate[List[MessageV3]]
      messageResult shouldBe an[JsSuccess[_]]
      messageResult.get shouldBe a[List[_]]
      messageResult.get shouldBe a[List[_]]
      messageResult.get.head shouldBe a[MessageV3]
      messageResult.get.head.validFrom.toString should be("2013-12-01")
    }
  }
}
