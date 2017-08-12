package org.jabref.logic.sharelatex;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ShareLatexJsonMessage {

    public String createDeleteInsertMessage(String docId, int position, int version, String oldContent, String newContent) {

        JsonObject insertContent = new JsonObject();
        insertContent.addProperty("p", position);
        insertContent.addProperty("d", oldContent);

        JsonObject deleteContent = new JsonObject();
        deleteContent.addProperty("p", position);
        deleteContent.addProperty("i", newContent);

        JsonArray opArray = new JsonArray();
        opArray.add(insertContent);
        opArray.add(deleteContent);

        JsonObject docIdOp = new JsonObject();
        docIdOp.addProperty("doc", docId);
        docIdOp.add("op", opArray);
        docIdOp.addProperty("v", version);

        JsonArray argsArray = new JsonArray();
        argsArray.add(docId);
        argsArray.add(docIdOp);

        JsonObject obj = new JsonObject();
        obj.addProperty("name", "applyOtUpdate");
        obj.add("args", argsArray);

        return obj.toString();

    }

    public String createUpdateMessageAsInsertOrDelete(String docId, int version, List<SharelatexDoc> docs) {
        JsonArray opArray = new JsonArray();
        for (SharelatexDoc doc : docs) {
            JsonObject deleteOrInsertContent = new JsonObject();
            deleteOrInsertContent.addProperty("p", doc.getPosition());
            deleteOrInsertContent.addProperty(doc.getOperation(), doc.getContent());
            opArray.add(deleteOrInsertContent);
        }

        JsonObject docIdOp = new JsonObject();
        docIdOp.addProperty("doc", docId);
        docIdOp.add("op", opArray);
        docIdOp.addProperty("v", version);

        JsonArray argsArray = new JsonArray();
        argsArray.add(docId);
        argsArray.add(docIdOp);

        JsonObject obj = new JsonObject();
        obj.addProperty("name", "applyOtUpdate");
        obj.add("args", argsArray);

        return obj.toString();
    }

}
