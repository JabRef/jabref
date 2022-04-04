# Functional Specification Document "Better Search" Feature for JabRef

## 1. Introduction

### 1.1 Vision and Goals
<!-- Beschreibt den Zweck und den Leserkreis der Spezifikation. -->
- The open-sourced citation and reference managment software JabRef is in need of a more functional global search and a user-friendly interface thereof.
- The users of JabRef should be able to conveniently search in their citations library and perform complex search querys, also via GUI.
- The goal of this project is to develop an implementation that improves upon the current library search capabilities and enables users of the JabRef software to very easily navigate their libraries. 

### 1.2 Scope Conditions
<!-- Gibt an, wo die Software eingesetzt werden soll und welche wesentlichen Funktionen es haben wird. Wo sinnvoll, sollte auch definiert werden, was die Software nicht leisten wird.
Beschreibt die mit der Software verfolgten Ziele. -->
- "Better Search" is a feature implementation for the citation and reference management software JabRef.
- Target group of the JabRef software are researchers, PHD candidates, university members, students and the scientific community in general.
- An internet conneciton is not needed for this feature.
- Search queries in global search must have the same syntax as the already implemented web search. 
- Search queries need to be performed reliably and without any bugs or errors.


### 1.3 Definitions
<!-- Dokumentiert alle verwendeten Fachbegriffe und Abkürzungen. Alternativ können Sie auch ein separates Glossar nutzen. -->
- GUI: Graphical User Interface
- Cross-platform: 
- Dropdown menu:
- Search queries:


### 1.4 Overview
<!-- Beschreibt, wie der Rest der Spezifikation aufgebaut ist, insbesondere, wie Kapitel 3 strukturiert ist. -->
- The search strings in the global search bar is parsed to an existing QueryParser.
- The search user interface is a dropdown menu.

## 2. General Description

### 2.1 Implementation
<!-- Beschreibt, wie das System in seine Umgebung eingebettet ist und wie die Software mit den umgebenden Komponenten und Systemen zusammenspielt. Dazu werden die Schnittstellen, Kommunikationsprotokolle etc. definiert. -->


### 2.2 Function Overview
<!-- Skizziert die wichtigsten Funktionen -->


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
* Name: Click on global search bar
* Stakeholders: Users of JabRef
* Preconditions: JabRef must be started
* Procedure:
  * Click on the global search bar
* Postconditions: Dropdown menu shows up below the search bar with suggestions for syntax use. Last seen searches are listed.
* Postconditions exception: Dropdown menu shows up below the search bar with suggestions for syntax use. No prior searches show empty "Last searches" list

### Use Case 2:
<!-- * Name: *Name des Use-cases*
* Akteure: *Akteur1, Akteur2, ...*
* Vorbedingungen: *Was muss vor Beginn des Ablaufs gelten*
* Standardablauf
    * Schritt 1
    * Schritt 2
* Nachbedingungen Erfolg: *Was muss nach dem Ende des erfolgreichen Ablaufs gelten*
* Nachbedingung Sonderfall: *Was gilt nach dem Ende, wenn der Ablauf fehlgeschlagen ist* -->

## Appendix A. Open Questions
- AND/OR buttons?
- 