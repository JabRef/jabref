package org.jabref.model.sharelatex;


import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Arg {

    @SerializedName("doc") @Expose private String doc;
    @SerializedName("op") @Expose private List<Op> op = new ArrayList<>();
    @SerializedName("v") @Expose private int version;

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public List<Op> getOp() {
        return op;
}

    public void setOp(List<Op> op) {
        this.op = op;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int v) {
        this.version = v;
    }


}