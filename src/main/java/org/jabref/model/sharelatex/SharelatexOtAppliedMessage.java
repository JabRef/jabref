package org.jabref.model.sharelatex;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SharelatexOtAppliedMessage {

    @SerializedName("name") @Expose private String name;
    @SerializedName("args") @Expose private List<Arg> args = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Arg> getArgs() {
        return args;
    }

    public void setArgs(List<Arg> args) {
        this.args = args;
    }

}