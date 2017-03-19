package com.istratenko.searcher.tokenizer;


import com.mongodb.BasicDBObject;

import java.io.Serializable;

/**
 * Created by denis on 19.03.17.
 */
public class Positions implements Serializable{

    private String document;
    private Integer startSymbol;
    private Integer endSymbol;
    private Integer numberOfLine;


    public Positions(){

    }

    public Positions(String document, int numberOfLine, int startSymbol, int endSymbol){
        this.startSymbol=startSymbol;
        this.endSymbol=endSymbol;
        this.numberOfLine=numberOfLine;
        this.document=document;
    }

    public Integer getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(int startSymbol) {
        this.startSymbol = startSymbol;
    }

    public Integer getEndSymbol() {
        return endSymbol;
    }

    public void setEndSymbol(int endSymbol) {
        this.endSymbol = endSymbol;
    }

    public Integer getNumberOfLine() {
        return numberOfLine;
    }

    public void setNumberOfLine(int numberOfLine) {
        this.numberOfLine = numberOfLine;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}
