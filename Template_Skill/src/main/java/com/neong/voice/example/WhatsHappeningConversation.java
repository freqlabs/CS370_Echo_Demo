package com.neong.voice.example;

import com.amazon.speech.slu.Intent;

import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;

import com.neong.voice.model.base.Conversation;

public class WhatsHappeningConversation extends Conversation {
    private final static String INTENT_WHATS_HAPPENING = "WhatsHappeningIntent";

    public WhatsHappeningConversation() {
        super();

        supportedIntentNames.add(INTENT_WHATS_HAPPENING);
    }

    @Override
    public SpeechletResponse respondToIntentRequest(IntentRequest intentReq, Session session) {
        return newTellResponse("I have no idea what's happening!", false);
    }
}
