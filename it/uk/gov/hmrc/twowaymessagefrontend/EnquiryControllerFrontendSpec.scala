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

package uk.gov.hmrc.twowaymessagefrontend


import com.google.inject.AbstractModule
import connectors.{PreferencesConnector, TwoWayMessageConnector}
import controllers.EnquiryController
import models.EnquiryDetails
import net.codingwell.scalaguice.ScalaModule
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.openqa.selenium.JavascriptExecutor
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Reads}
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.OptionalRetrieval
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.twowaymessagefrontend.util.{ControllerSpecBase, MockAuthConnector}

import scala.concurrent.Future

class EnquiryControllerFrontendSpec extends ControllerSpecBase  with MockAuthConnector with HtmlUnitFactory with   OneBrowserPerSuite{


  val preferencesConnector: PreferencesConnector = mock[PreferencesConnector]
  val twoWayMessageConnector: TwoWayMessageConnector = mock[TwoWayMessageConnector]

  when(twoWayMessageConnector.getMessages(any())(any())).thenReturn(Future.successful(List()))

  override lazy val injector = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> false, "testserver.port" -> 8990))
    .overrides(new AbstractModule with ScalaModule {
      override def configure(): Unit = {
        bind[AuthConnector].toInstance(mockAuthConnector)
        bind[PreferencesConnector].toInstance(preferencesConnector)
        bind[TwoWayMessageConnector].toInstance(twoWayMessageConnector)
      }
    })
    .injector()

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(Configuration("metrics.enabled" -> false))
      .overrides(new AbstractModule with ScalaModule {
        override def configure(): Unit = {
          bind[AuthConnector].toInstance(mockAuthConnector)
          bind[PreferencesConnector].toInstance(preferencesConnector)
          bind[TwoWayMessageConnector].toInstance(twoWayMessageConnector)
        }
      }).build()
  }

  val enquiryController = injector.instanceOf[EnquiryController]

  "Frontend test" should {
    "find the home page ok" in {
      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))
      when( preferencesConnector.getPreferredEmail( ArgumentMatchers.eq("AB123456C") )(ArgumentMatchers.any[HeaderCarrier])) thenReturn {
        Future.successful("email@dummy.com")
      }


      val result = await(call(enquiryController.onPageLoad("p800"), fakeRequest))
      result.header.status mustBe (200)
    }

    "Forbidden is we dont have a NINO" in {
      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(None))


      val result = await(call(enquiryController.onPageLoad("p800"), fakeRequest))
      result.header.status mustBe (403)
    }


    "Send a valid message" in {
      import org.mockito.Mockito._

      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some("AB123456C")))

      when( preferencesConnector.getPreferredEmail( ArgumentMatchers.eq("AB123456C") )(ArgumentMatchers.any[HeaderCarrier])) thenReturn {
        Future.successful("email@dummy.com")
      }

      val enquiryDetails = EnquiryDetails("p800", "A question", "A question from the customer", "test@dummy.com", "test@dummy.com")
      when(twoWayMessageConnector.postMessage( ArgumentMatchers.eq(enquiryDetails))(ArgumentMatchers.any[HeaderCarrier])) thenReturn {
        val x = Json.parse( """{ "id":"5c18eb166f0000110204b160" }""".stripMargin )

        Future.successful(HttpResponse(play.api.http.Status.CREATED, Some(x)))
      }

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      textField("subject").value = "A question"
      textField("email").value = "test@dummy.com"
      textField("confirmEmail").value = "test@dummy.com"
      textArea("content").value = "A question from the customer"


      click on find(id("submit")).value

      eventually { pageSource must include ("HMRC received your message and will reply within") }
    }

    "Send a valid reply message" in {
      import org.mockito.Mockito._

      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some("AB123456C")))

      when( preferencesConnector.getPreferredEmail( ArgumentMatchers.eq("AB123456C") )(ArgumentMatchers.any[HeaderCarrier])) thenReturn {
        Future.successful("email@dummy.com")
      }

      val enquiryDetails = EnquiryDetails("p800", "A question", "A question from the customer", "test@dummy.com", "test@dummy.com")
      when(twoWayMessageConnector.postReplyMessage( ArgumentMatchers.any(), ArgumentMatchers.eq("p800"), ArgumentMatchers.eq("5c8a31931d00000b00a30bdc"))(ArgumentMatchers.any[HeaderCarrier])).thenReturn {
        val x = Json.parse( """{ "id":"5c18eb166f0000110204b160" }""".stripMargin )
        Future.successful(HttpResponse(play.api.http.Status.CREATED, Some(x)))
      }

      go to s"http://localhost:$port/two-way-message-frontend/message/customer/p800/5c8a31931d00000b00a30bdc/reply"

      textArea("content").value = "A question from the customer"

      click on find(id("submit")).value

      eventually { pageSource must include ("HMRC received your message and will reply within") }
    }

  }

  "Subject field" should {

    "display error message if nothing entered" in {
      stubLogin("AB123456C")

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      click on find(id("submit")).value

      eventually { pageSource must include ("Please enter a subject") }
    }

//    "display error message if subject is longer than max" in {
//      stubLogin("AB123456C")
//
//      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"
//
//      textField("subject").value = Random.nextString(200)
//
//      click on find(id("submit")).value
//
//      eventually { pageSource must include ("Subject has a maximum length of 65 characters") }
//    }



  }

  "email fields" should {
    "display an error if nothing entered in email and confirmEmail" in {
      stubLogin("AB123456C")

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      textField("email").value = ""
      textField("confirmEmail").value = ""

      click on find(id("submit")).value

      eventually { pageSource must include ("Email is invalid") }
    }


    "display an error if something entered in email and nothing in confirmEmail" in {
      stubLogin("AB123456C")

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      textField("email").value = "test@test.com"
      textField("confirmEmail").value = ""

      click on find(id("submit")).value

      eventually { pageSource must include ("Email is invalid") }
    }

    "display an error if nothing entered in email and something in confirmEmail" in {
      stubLogin("AB123456C")

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      textField("email").value = ""
      textField("confirmEmail").value = "test@test.com"

      click on find(id("submit")).value

      eventually { pageSource must include ("Email is invalid") }
    }

    "display an error if email and confirmEmail do not match" in {
      stubLogin("AB123456C")

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      textField("email").value = "email@email.com"
      textField("confirmEmail").value = "confirmEmail@confirmEmail.com"

      click on find(id("submit")).value

      eventually { pageSource must include ("Email addresses must match. Check them and try again.") }
    }


    "should include preferances email addr as we have it" in {
      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some("AB123456C")))

      when( preferencesConnector.getPreferredEmail( ArgumentMatchers.eq("AB123456C") )(ArgumentMatchers.any[HeaderCarrier])) thenReturn {
        Future.successful("email@dummy.com")
      }

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      eventually { textField("email").value must include ("email@dummy.com") }
      eventually { textField("confirmEmail").value must include ("email@dummy.com") }
    }



    "content field"  should {
      "display error if nothing entered" in {
        stubLogin("AB123456C")

        go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

        textArea("content").value = ""

        click on find(id("submit")).value

        eventually { pageSource must include ("Please enter a question") }
      }


//      "display error if long message entered" in {
//        stubLogin("AB123456C")
//
//        go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"
//
//        setContentToStringOfLength("content", 75201 )
//
//        click on find(id("submit")).value
//
//        eventually { pageSource must include ("Content has a maximum length of 75,000 characters") }
//      }

    }
  }


  private def setContentToStringOfLength(id:String, length:Int): Unit = {
    val js = s"""document.getElementById("$id").value = 'x'.repeat($length)"""
    val j = webDriver.asInstanceOf[JavascriptExecutor]
    j.executeScript(js)
  }



  def stubLogin( nino:String): Unit = {
    mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some(nino)))
    mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some("AB123456C")))

    when( preferencesConnector.getPreferredEmail( ArgumentMatchers.eq("AB123456C") )(ArgumentMatchers.any[HeaderCarrier])) thenReturn {
      Future.successful("")
    }
  }



  "other endpoints" should {
    "call messagesRedirect for redirection" in {
      val result = await(call(enquiryController.messagesRedirect, fakeRequest))
      result.header.status mustBe (303)
    }

    "call personalAccountRedirect for redirection" in {
      val result = await(call(enquiryController.messagesRedirect, fakeRequest))
      result.header.status mustBe (303)
    }
  }

}