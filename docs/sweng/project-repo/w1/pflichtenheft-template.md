---
layout: default
title : Woche 6
---
# Pflichtenheft
#####  (Nach Lichter & Ludwig, Software Engineering: Grundlagen, Menschen, Prozesse, Techniken)


## 1. Einleitung

### 1.1 Zweck

Beschreibt den Zweck und den Leserkreis der Spezifikation.

### 1.2 Einsatzbereich und Ziele
Gibt an, wo die Software eingesetzt werden soll und welche wesentlichen Funktionen es haben wird. Wo sinnvoll, sollte auch definiert werden, was die Software nicht leisten wird.

Beschreibt die mit der Software verfolgten Ziele.

### 1.3 Definitionen

Dokumentiert alle verwendeten Fachbegriffe und Abkürzungen. Alternativ können Sie auch ein separates Glossar nutzen.

### 1.4 Referenzierte Dokumente

Verzeichnet alle Dokumente, auf die in der Spezifikation verwiesen wird.

### 1.5 Überblick

Beschreibt, wie der Rest der Spezifikation aufgebaut ist, insbesondere, wie Kapitel 3 strukturiert ist.

## 2. Allgemeine Beschreibung

### 2.1 Einbettung

Beschreibt, wie das System in seine Umgebung eingebettet ist und wie die Software mit den umgebenden Komponenten und Systemen zusammenspielt. Dazu werden die Schnittstellen, Kommunikationsprotokolle etc. definiert.

### 2.2 Funktionen

Skizziert die wichtigsten Funktionen

### 2.3 Benutzerprofile

Charakterisiert die Benutzergruppen und die Voraussetzungen die diese jeweils mitbringen (Ausbildung, Know-how, Sprache)

### 2.4 Einschränkungen
Dokumentiert Einschränkungen, die die Freiheit der Entwicklung reduzieren (Basis-Software, Ziel-Hardware, Gesetzliche Grundlagen, ...)

### 2.5 Annahmen und Abhängigkeiten
Nennt explizit die Annahmen und externen Voraussetzungen, von denen bei der Spezifikation ausgegangen wurde.


## 3. Einzelanforderungen

Beschreibt die Anforderung i so genau, dass bei der Verwendung der Spezifikation (im Entwurf usw.) keine Rückfragen dazu notwendig sind.

Identifizieren Sie jede Funktionale Anforderung mit einer Nummer, so dass diese Nachverfolgbar sind. Zusammengehörende Funktionale Anforderungen können durch geeignete Nummerierung angezeigt werden.

Zur Spezifikation der Software sollen Sprachschablonen benutzt werden.

* /F10/ Funktion 1 des Systems
* /F11/ Weitere Detaillierung Funkion 1
* /F20/ Funktion 2 des Systems


Die Funktionalen Anforderungen sollen mithilfe von Use-cases erhoben werden. Die Use-cases sollen in Anhang A detailliert beschrieben werden.

## 4. Abnahmekriterien

Beschreiben Sie hier, wie die Anforderungen bei der Abnahme auf ihre Realisierung überprüft werden können.

Definieren Sie hier mindestens ein Abnahmekriterium
* /A10/ Abnahmekriterium 1
* /A20/ Abnahmekriterium 2


# Anhang

## Anhang A. Use-cases

An dieser Stelle können detaillierte Use-cases angegeben werden
![Diagram](../../slides/images/use-case.png)

### Use Case 1:
* Name: *Name des Use-cases*
* Akteure: *Akteur1, Akteur2, ...*
* Vorbedingungen: *Was muss vor Beginn des Ablaufs gelten*
* Standardablauf
    * Schritt 1
    * Schritt 2
* Nachbedingungen Erfolg: *Was muss nach dem Ende des erfolgreichen Ablaufs gelten*
* Nachbedingung Sonderfall: *Was gilt nach dem Ende, wenn der Ablauf fehlgeschlagen ist*


#### Sonderfall 1a: Ausnahme 1
* Ablauf Sonderfall 1a
    * Schritt 1
    * Schritt 2

#### Sonderfall 1b: Ausnahme 2
* Ablauf Sonderfall 1b
    * Schritt 1
    * Schritt 2


### Use Case 2:
* Name: *Name des Use-cases*
* Akteure: *Akteur1, Akteur2, ...*
* Vorbedingungen: *Was muss vor Beginn des Ablaufs gelten*
* Standardablauf
    * Schritt 1
    * Schritt 2
* Nachbedingungen Erfolg: *Was muss nach dem Ende des erfolgreichen Ablaufs gelten*
* Nachbedingung Sonderfall: *Was gilt nach dem Ende, wenn der Ablauf fehlgeschlagen ist*

#### Sonderfall 2a: Ausnahme 1
* Ablauf Sonderfall 1a
    * Schritt 1
    * Schritt 2
