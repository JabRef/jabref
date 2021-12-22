# Jacoco Test Coverage Anleitung

######wenn du deinen aktuellen branch mit dem jacoco-test überprüfen willst, dann kannst du einen der beiden Wege gehen:

### Methode 1

 

- du musst die beiden files "myproject.jacoco-aggregation" und "myproject.java-conventions" ins src/main/groovy reinmachen
- danach ins terminal "./gradlew jacocoTestReport" ausführen (achtung ist systemabhängig ob windows powershell oder sonst was eingestellt ist)

### Methode 2

- führe den befehl "./gradlew jacocoTestReport" direkt im terminal aus

### Methode 3

- gehe zu den "edit configurations" und wähle deine jabref-application
- rechts daneben drückst du auf die schaltfläche "modify options" und wählst aus der liste "specify alternative coverage runner" aus
- es erscheint ein dropdown menu auf welches du klicken musst und dann wählst du jacoco aus 
- apply und oke drücken und auf das run-symbol clicken