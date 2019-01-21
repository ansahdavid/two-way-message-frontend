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

package connectors

import javax.inject.{Inject, Singleton}
import models.{ContactDetails, InquiryDetails, TwoWayMessage}
import play.api.Mode.Mode
import play.api.http.Status
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TwoWayMessageConnector @Inject()(httpClient: HttpClient,
                                       override val runModeConfiguration: Configuration,
                                       val environment: Environment)(implicit ec: ExecutionContext)
  extends Status with ServicesConfig {

  override protected def mode: Mode = environment.mode
  private val logger = Logger(this.getClass)
  lazy val twoWayMessageBaseUrl: String = baseUrl("two-way-message")

  def postMessage(details: InquiryDetails)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val message = TwoWayMessage(
      ContactDetails(details.email),
      details.subject,
      details.text
    )
    httpClient.POST(s"$twoWayMessageBaseUrl/two-way-message/message/customer/${details.queue}/submit", message)
  }
}