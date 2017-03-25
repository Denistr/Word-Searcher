package com.istratenko.searcher.entity;


import com.mongodb.BasicDBObject;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Created by denis on 19.03.17.
 */
public class Positions  implements Serializable, Comparable<Positions> {

    private String document;
    private Integer line;
    private Integer start;
    private Integer end;

    public Positions() {

    }

    public Positions(String document, int line, int start, int end) {
        this.start = start;
        this.end = end;
        this.line = line;
        this.document = document;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
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

        Positions positions = (Positions) o;

        if (document != null ? !document.equals(positions.document) : positions.document != null) return false;
        //if (!line.equals(positions.line)) return false;
        if (!start.equals(positions.start)) return false;
        return end.equals(positions.end);

    }

    @Override
    public int hashCode() {
        int result = document != null ? document.hashCode() : 0;
        //result = 31 * result + line.hashCode();
        result = 31 * result + start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Positions{" +
                "document='" + document + '\'' +
                ", line=" + line +
                ", start=" + start +
                ", end=" + end +
                '}';
    }


    @Override
    public int compareTo(Positions p) {
        return Comparators.POSITIONS.compare(this, p);
    }

    public static class Comparators {

        public static Comparator<Positions> POSITIONS = new Comparator<Positions>() {
            @Override
            public int compare(Positions p1, Positions p2) {
                if (p1.getDocument().compareTo(p2.getDocument())==0){
                    return p1.getStart().compareTo(p2.getStart());
                }
                return p1.getDocument().compareTo(p2.getDocument());
            }
        };
    }
}
