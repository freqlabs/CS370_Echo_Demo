package com.neong.voice.example

import com.amazon.speech.speechlet.{
  IntentRequest,
  Session,
  SpeechletResponse
}
import com.neong.voice.model.base.Conversation

class HappeningTodayConversation extends Conversation {
  import com.neong.voice.example.HappeningTodayConversation._

  supportedIntentNames.add(HappeningTodayIntent)

  override def respondToIntentRequest(
    intentReq: IntentRequest,
    session: Session
  ): SpeechletResponse = {
    Conversation.newTellResponse("Stuff.", false)
  }
}

object HappeningTodayConversation {
  val HappeningTodayIntent = "HappeningTodayIntent"
}
