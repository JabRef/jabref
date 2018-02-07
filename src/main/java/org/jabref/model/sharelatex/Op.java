package org.jabref.model.sharelatex;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Op {

    @SerializedName("p") @Expose private int position;
    @SerializedName(value = "d", alternate = "i") @Expose private String chars;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getChars() {
        return chars;
    }

    public void setOp(String op) {
        this.chars = op;
    }

}
