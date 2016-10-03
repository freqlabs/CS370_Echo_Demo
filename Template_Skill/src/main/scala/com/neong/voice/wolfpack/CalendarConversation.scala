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
      Conversation.newTellResponse(responseSsml.toString, true)
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
    import scala.xml._

    def nextEventSsml(db: DbConnection, limit: Integer = 5): Elem = {
      val result = db.runQuery(
        "SELECT count(*), summary, start, name " +
          "FROM event_info " +
          "WHERE start = (" +
            "SELECT start FROM event_info WHERE start > now() LIMIT 1" +
          ") " +
          "GROUP BY summary, start, name " +
          s"LIMIT $limit;"
      )

      Option(result) match {
        case Some(events) => eventsSsml(events, limit)
        case None => noEventsSsml
      }
    }

    private type ResultsMap =
      java.util.Map[String, java.util.Vector[Object]]

    private val noEventsSsml =
      <speak>Sorry, there's nothing coming up on the calendar.</speak>

    private def eventsSsml(events: ResultsMap, limit: Int): Elem = {
      val count = events.get("count").get(0).asInstanceOf[Long].toInt
      <speak>
        OK, the next event{if (count > 1) "s are" else " is"}
        {for (i <- 0 until count) yield eventSsml(events, i)}
        {if (count > limit)
          <s>There are {count - limit} more events starting at this time.</s>
        }
      </speak>
    }

    private def eventSsml(events: ResultsMap, index: Int): Elem = {
      val what = events.get("summary").get(index).asInstanceOf[String]
      val when = events.get("start").get(index).asInstanceOf[Timestamp]
      val location = events.get("name").get(index).asInstanceOf[String]

      val localDateTime = when.toLocalDateTime.atZone(PST.toZoneId)
      val date = localDateTime.toLocalDate
      val time = localDateTime.toLocalTime.format(TimeFormatter)

      val where = Option(location) match {
        case Some(place) => s"in $place"
        case None => ". No location specified."
      }

      <s>
        {what}
        on <say-as interpret-as="date" format="md">{date}</say-as>
        at <say-as interpret-as="time">{time}</say-as>
        {where}
      </s>
    }
  }
}
