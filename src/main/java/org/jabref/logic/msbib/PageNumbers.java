package org.jabref.logic.msbib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PageNumbers {
    private static final Pattern PAGE_PATTERN = Pattern.compile("\\s*(\\d+)\\s*-{1,2}\\s*(\\d+)\\s*");
    private String freeform;
    private int start;

    private int end;

    public PageNumbers(String pages) {
        parsePageNums(pages);
    }

    private void parsePageNums(String pages) {
        Matcher matcher = PAGE_PATTERN.matcher(pages);
        if (matcher.matches()) {
            start = Integer.parseInt(matcher.group(1));
            end = Integer.parseInt(matcher.group(2));
        } else {
            freeform = pages;
        }
    }

    public Element getDOMrepresentation(Document document) {
        Element result = document.createElement("extent");
        result.setAttribute("unit", "page");
        if (freeform == null) {
            Element tmpStart = document.createElement("start");
            Element tmpEnd = document.createElement("end");
            tmpStart.appendChild(document.createTextNode(String.valueOf(this.start)));
            tmpEnd.appendChild(document.createTextNode(String.valueOf(this.end)));
            result.appendChild(tmpStart);
            result.appendChild(tmpEnd);
        } else {
            Node textNode = document.createTextNode(freeform);
            result.appendChild(textNode);
        }
        return result;
    }

    public String toString(String separator) {
        if (freeform != null) {
            return freeform;
        }
        return start + separator + end;
    }

    @Override
    public String toString() {
        return toString("-");
    }

}
