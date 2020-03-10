# GraalREPL
REPL (read–eval–print loop) shell built ontop of JavaFX stack, GraalJS, GraalPython, TruffleRuby and FastR

## JVM workflow
` mvn clean javafx:run `

## Native-image workflow
Desktop: `mvn clean client:build client:run`
Android: `mvn -Pandroid clean client:build client:run`
iOS: `mvn -Pios clean client:build client:run`
