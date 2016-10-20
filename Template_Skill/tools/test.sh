#!/bin/sh

curl -k https://localhost:8443/Template_Skill/ssunews --data-binary '
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
' 2>/dev/null | json_pp
