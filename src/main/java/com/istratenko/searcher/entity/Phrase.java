package com.istratenko.searcher.entity;

/**
 * Created by denis on 23.03.17.
 */
public class Phrase {
    private String document;
    private int start;
    private int end;

    public Phrase(){}

    public Phrase(String document, int start, int end){
        this.document=document;
        this.start=start;
        this.end=end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Phrase phrase = (Phrase) o;

        if (start != phrase.start) return false;
        if (end != phrase.end) return false;
        return document.equals(phrase.document);

    }

    @Override
    public int hashCode() {
        int result = document.hashCode();
        result = 31 * result + start;
        result = 31 * result + end;
        return result;
    }
}
