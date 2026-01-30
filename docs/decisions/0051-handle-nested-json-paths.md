---
nav_order: 51
parent: Decision Records
---
<!-- markdownlint-disable-next-line MD025 -->
# How to handle nested JSON paths

## Context and Problem Statement

When trying to parse nested JSON structures in JabLS which receives the VSCode settings as (partial) JSON, we need to decide how to handle nested JSON paths.

## Considered Options

* Use `org.hisp.dhis:json-tree`
* Use Jackson
* Use Unirest and Optional
* Use GSON and chaining Optional
* Use an own method to parse the path

## Decision Outcome

Chosen option: "Use Jackson" because it comes out best (see below).

## Pros and Cons of the Options

### Use `org.hisp.dhis:json-tree`

```java
this.integrityCheck = json.getBoolean("jabref.integrityCheck.enabled").booleanValue(this.integrityCheck);
```

* Good, because very compact and readable
* Good, because no Exception is thrown if path does not exist
* Good, because it supports nested paths directly
* Good, because it has a fallback value
* Bad, because it introduces a new dependency

### Use Jackson

```java
this.consistencyCheck = node.at("/jabref/consistencyCheck/enabled").asBoolean(this.consistencyCheck);
```

* Good, because very compact and readable
* Good, because no Exception is thrown if path does not exist
* Good, because it supports nested paths directly
* Good, because it has a fallback value

### Use Unirest and Optional

```java
this.integrityCheck = Optional.ofNullable(json.optJSONObject("jabref"))
                              .map(jabref -> jabref.optJSONObject("integrityCheck"))
                              .map(integrityCheck -> integrityCheck.optBoolean("enabled", this.integrityCheck))
                              .orElse(this.integrityCheck);
```

* Good, because no Exception is thrown if path does not exist
* Good, because it has a fallback value
* Bad, because it is quite verbose
* Bad, because it requires chaining Optional

### Use GSON and chaining Optional

```java
this.integrityCheck = Optional.ofNullable(json.get("jabref"))
                              .map(JsonElement::getAsJsonObject)
                              .map(jsonObject -> jsonObject.get("integrityCheck"))
                              .map(JsonElement::getAsJsonObject)
                              .map(jsonObject -> jsonObject.get("enabled"))
                              .map(JsonElement::getAsBoolean)
                              .orElse(this.integrityCheck);
```

* Good, because no Exception is thrown if path does not exist
* Good, because it has a fallback value
* Bad, because it is verbose
* Bad, because it requires chaining Optional

### Use an own method to parse the path

```java
private <T> T assignIfPresent(JsonObject obj, T current, Class<T> type, String... path) {
    JsonObject currentObject = obj;
    for (String key : path) {
        Optional<JsonElement> element = Optional.ofNullable(currentObject.get(key));
        if (element.isEmpty()) {
            return current;
        }
        if (element.get().isJsonObject()) {
            currentObject = element.get().getAsJsonObject();
            continue;
        }
        try {
            T value = gson.fromJson(element.get(), type);
            if (value != null) {
                return value;
            }
        } catch (JsonParseException _) {
            return current;
        }
    }
    return current;
}
```

* Good, because no Exception is thrown if path does not exist
* Good, because it has a fallback value
* Bad, because it is very verbose
* Bad, because it needs way more parameters
* Bad, because it needs to be tested properly
* Bad, because it needs to be maintained
