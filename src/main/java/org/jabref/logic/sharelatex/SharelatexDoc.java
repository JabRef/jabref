package org.jabref.logic.sharelatex;

import java.util.Objects;

public class SharelatexDoc {

    private int position;
    private String content;
    private String operation;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String opType) {
        this.operation = opType;
    }

    @Override
    public String toString() {
        return operation + " " + position + " " + content;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SharelatexDoc other = (SharelatexDoc) obj;

        return Objects.equals(content, other.content) && Objects.equals(position, other.position) && Objects.equals(operation, other.operation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, position, operation);
    }

}
