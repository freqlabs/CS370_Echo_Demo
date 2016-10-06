(defproject Template_Skill "1.0"
  :description "An Amazon Alexa skill for Sonoma State University"
  :min-lein-version "2.0.0"
  :dependencies [[org.postgresql/postgresql "9.4.1211"]
                 [com.amazon.alexa/alexa-skills-kit "1.1.3"]
                 [com.amazonaws/aws-lambda-java-core "1.1.0"]
                 [org.clojure/clojure "1.8.0"]]
  :repositories [["jcenter" {:url "http://jcenter.bintray.com"}]]
  :plugins [[lein-pprint "1.1.2"]]

  :omit-source true
  :java-source-paths ["src/main/java"]
  :javac-options ["-target" "1.8" "-source" "1.8"]

  :resource-paths ["src/main/resources"]

  :prep-tasks ["clean"
               ["with-profile" "firstpass-j" "javac"]
               ["with-profile" "firstpass-c" "compile"]
               "javac"]

  :profiles {:firstpass-c {:prep-tasks ^:replace []
                           :source-paths ^:replace ["src/main/clojure"]
                           :aot [com.neong.voice.wolfpack.CalendarConversation]}
             :firstpass-j {:prep-tasks ^:replace []
                           :java-source-paths ^:replace ["src/main/java/com/neong/voice/model"
                                                         "src/main/java/com/wolfpack"]}})
