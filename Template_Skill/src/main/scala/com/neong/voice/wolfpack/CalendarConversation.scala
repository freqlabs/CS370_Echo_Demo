package com.neong.voice.wolfpack

import collection.JavaConverters._

import com.neong.voice.model.base.Conversation

import com.wolfpack.database.DbConnection

class CalendarConversation extends Conversation {
  import com.amazon.speech.speechlet.{
    IntentRequest,
    Session,
    SpeechletResponse
  }

  import CalendarConversation._
  import ConversationIntent._

  for (intent <- ConversationIntent.values) {
    supportedIntentNames.add(intent.toString)
  }

  override def respondToIntentRequest(
    intentReq: IntentRequest,
    session: Session
  ): SpeechletResponse = {
    val intent = intentReq.getIntent
    val conversationIntent = ConversationIntent.withName(intent.getName)

    val db = new DbConnection("DbCredentials.xml")

    if (db.getRemoteConnection) {
      val responseSsml = conversationIntent match {
        case NextEventIntent => CalendarDataProvider.nextEventSsml(db)
      }
      Conversation.newTellResponse(responseSsml, true)
    } else {
      Conversation.newTellResponse("Sorry, I'm on break right now.", false)
    }
  }
}

object CalendarConversation {
  import java.sql.Timestamp
  import java.time.LocalDateTime
  import java.time.format.DateTimeFormatter
  import java.util.TimeZone

  object ConversationIntent extends Enumeration {
    type ConversationIntent = Value
    val NextEventIntent = Value("NextEventIntent")
  }

  private final val PST = TimeZone.getTimeZone("America/Los Angeles")
  private final val TimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
  private object CalendarDataProvider {
    def nextEventSsml(db: DbConnection): String = {
      val event = db.runQuery(
        "SELECT summary, start, name " +
          "FROM ssucalendar.event_info " +
          "WHERE start > now() " +
          "LIMIT 1;"
      )

      val what = event.get("summary").get(0).asInstanceOf[String]
      val when = event.get("start").get(0).asInstanceOf[Timestamp]
      val location = event.get("name").get(0).asInstanceOf[String]

      val localDateTime = when.toLocalDateTime.atZone(PST.toZoneId)
      val date = localDateTime.toLocalDate
      val time = localDateTime.toLocalTime.format(TimeFormatter)

      val where = Option(location) match {
        case Some(place) => s"in $place"
        case None => ". No location specified."
      }

      val ssml =
        <speak>
          OK, the next event is {what}
          on <say-as interpret-as="date" format="md">{date}</say-as>
          at <say-as interpret-as="time">{time}</say-as>
          {where}.
        </speak>

      ssml.toString
    }
  }
}
