# Functional Specification Document

## 1. Introduction

### 1.1 Purpose
<!-- Beschreibt den Zweck und den Leserkreis der Spezifikation. -->

- The actual purpose of Autocomplete is to assist the user in making entries in the Global Search Bar. This makes it easier to use and makes working in Jabref pleasant.
- Auto-complete suggestions must be displayed almost immediately after the user starts typing, with no delay. By providing suggestions quickly, the interface looks more interactive which improves the user experience.
    To do this, we will highlight the differences to ensure that users can easily read and distinguish the suggestions, you should make the differences clear by highlighting or bolding them.

### 1.2 Scope
<!-- Gibt an, wo die Software eingesetzt werden soll und welche wesentlichen Funktionen es haben wird. Wo sinnvoll, sollte auch definiert werden, was die Software nicht leisten wird.
Beschreibt die mit der Software verfolgten Ziele. -->

- Auto-completion is case sensitive there should be a function which must distinguish for example at "prote", you may get "protein" but not "protease" if both are present.
- A mechanism to move to the next match also belongs in such an application, this should be easily executable via the tabulator.
- Autocomplete should correct typos by itself and thus create a pleasant way of working as mentioned above.
- All words to be matched should already be available in the database of Jabref so that an internet connection is not necessary.

### 1.3 Definitions
<!-- Dokumentiert alle verwendeten Fachbegriffe und Abkürzungen. Alternativ können Sie auch ein separates Glossar nutzen. -->


### 1.4 References
<!-- Verzeichnet alle Dokumente, auf die in der Spezifikation verwiesen wird. -->


### 1.5 Overview
<!-- Beschreibt, wie der Rest der Spezifikation aufgebaut ist, insbesondere, wie Kapitel 3 strukturiert ist. -->


## 2. General Description

### 2.1 Implementation
<!-- Beschreibt, wie das System in seine Umgebung eingebettet ist und wie die Software mit den umgebenden Komponenten und Systemen zusammenspielt. Dazu werden die Schnittstellen, Kommunikationsprotokolle etc. definiert. -->


### 2.2 Function Overview
<!-- Skizziert die wichtigsten Funktionen -->
The autocompletions should also be able to be shared among concurrent users of the same database, so that there is consistency.

One of JabRef's predecessors, Bibkeeper, had an autocomplete feature based on collecting all words used for the same field in the same or all open databases. This allowed suggestions to be made based on what keywords, etc. were used in other entries.

### 2.3 User Requirements
<!-- Charakterisiert die Benutzergruppen und die Voraussetzungen die diese jeweils mitbringen (Ausbildung, Know-how, Sprache) -->


### 2.4 Limitations
<!-- Dokumentiert Einschränkungen, die die Freiheit der Entwicklung reduzieren (Basis-Software, Ziel-Hardware, Gesetzliche Grundlagen, ...) -->


### 2.5 Assumptions and Dependencies
<!-- Nennt explizit die Annahmen und externen Voraussetzungen, von denen bei der Spezifikation ausgegangen wurde. -->


### 3. Functional Requirements
<!-- Beschreibt die Anforderung i so genau, dass bei der Verwendung der Spezifikation (im Entwurf usw.) keine Rückfragen dazu notwendig sind.
Identifizieren Sie jede Funktionale Anforderung mit einer Nummer, so dass diese Nachverfolgbar sind. Zusammengehörende Funktionale Anforderungen können durch geeignete Nummerierung angezeigt werden.
Zur Spezifikation der Software sollen Sprachschablonen benutzt werden.

* /F10/ Funktion 1 des Systems
* /F11/ Weitere Detaillierung Funkion 1
* /F20/ Funktion 2 des Systems
Die Funktionalen Anforderungen sollen mithilfe von Use-cases erhoben werden. Die Use-cases sollen in Anhang A detailliert beschrieben werden. -->

- /F40/ Completes missing chars/words.
- /F41/ Completed text is selectable below the search bar.
- /F42/ Color-coded chars/words in the suggestions.
- /F43/ Correct typos and overwrite function.


### 4. Acceptance Criteria
<!-- Beschreiben Sie hier, wie die Anforderungen bei der Abnahme auf ihre Realisierung überprüft werden können.
Definieren Sie hier mindestens ein Abnahmekriterium
* /A10/ Abnahmekriterium 1
* /A20/ Abnahmekriterium 2 -->


# Appendix

## Appendix A. Use-cases
<!-- An dieser Stelle können detaillierte Use-cases angegeben werden
[Diagram](../../slides/images/use-case.png) -->

### Use Case 1:
<!-- * Name: *Name des Use-cases*
* Akteure: *Akteur1, Akteur2, ...*
* Vorbedingungen: *Was muss vor Beginn des Ablaufs gelten*
* Standardablauf
    * Schritt 1
    * Schritt 2
* Nachbedingungen Erfolg: *Was muss nach dem Ende des erfolgreichen Ablaufs gelten*
* Nachbedingung Sonderfall: *Was gilt nach dem Ende, wenn der Ablauf fehlgeschlagen ist* -->

### Use Case 2:

* Name: Autocomplete
* Stakeholders: Users of JabRef
* Preconditions: JabRef must be started
* Procedure:
    * Click on the global search bar.
    * Choose the fields you would like to search in (i.e. author, title etc.).
    * Type keywords to search for in search bar.
    * choose from the suggested words from the list displayed below the search bar.
* Postconditions:
  * After selection the word is transferred to the search bar and is ready for further entries
* Postconditions exception: 

## Appendix A. Open Questions
- 