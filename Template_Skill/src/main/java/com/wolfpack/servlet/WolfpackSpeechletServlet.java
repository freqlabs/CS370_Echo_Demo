package com.wolfpack.servlet;

import com.neong.voice.speechlet.TemplateBaseSkillSpeechlet;

import com.amazon.speech.speechlet.servlet.SpeechletServlet;

public class WolfpackSpeechletServlet extends SpeechletServlet {
    public WolfpackSpeechletServlet() {
        super();
        this.setSpeechlet(new TemplateBaseSkillSpeechlet());
    }
}
