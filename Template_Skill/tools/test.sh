#!/bin/sh

gradle appStart &

until echo exit | nc localhost 8080; do sleep 5; done

curl -s http://localhost:8080/Template_Skill/ssunews --data-binary '
{
    "session": {
        "sessionId": "SessionId.SOMEUUID",
        "application": {
            "applicationId": "amzn1.ask.skill.SOMEUUID"
        },
        "attributes": {},
        "user": {
            "userId": "amzn1.ask.account.SUPERSECRETLONGSTRING"
        },
        "new": true
    },
    "request": {
        "type": "IntentRequest",
        "requestId": "EdwRequestId.SOMEUUID",
        "locale": "en-US",
        "timestamp": "2016-10-19T21:26:10Z",
        "intent": {
            "name": "NextEventIntent",
            "slots": {}
        }
    },
    "version": "1.0"
}
' | json_pp

gradle appStop
