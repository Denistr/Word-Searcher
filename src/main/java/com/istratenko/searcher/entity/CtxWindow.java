package com.istratenko.searcher.entity;

import java.util.Comparator;

/**
 * Created by denis on 26.03.17.
 */
public class CtxWindow implements Comparable<CtxWindow>{
    String document;
    private Integer start;
    private Integer end;

    public CtxWindow(){}

    public CtxWindow(String document, int start, int end){
        this.document=document;
        this.start=start;
        this.end=end;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CtxWindow ctxWindow = (CtxWindow) o;

        if (!document.equals(ctxWindow.document)) return false;
        if (!start.equals(ctxWindow.start)) return false;
        return end.equals(ctxWindow.end);

    }

    @Override
    public int hashCode() {
        int result = document.hashCode();
        result = 31 * result + start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    @Override
    public int compareTo(CtxWindow o) {
        return Comparators.POSITIONS.compare(this, o);
    }

    public static class Comparators {

        public static Comparator<CtxWindow> POSITIONS = new Comparator<CtxWindow>() {
            @Override
            public int compare(CtxWindow p1, CtxWindow p2) {
                if (p1.getStart().compareTo(p2.getStart())==0){
                    return p1.getEnd().compareTo(p2.getEnd());
                }
                return p1.getStart().compareTo(p2.getStart());
            }
        };

        public static Comparator<CtxWindow> DOCUMENTS = new Comparator<CtxWindow>() {
            @Override
            public int compare(CtxWindow p1, CtxWindow p2) {
                if (p1.getDocument().compareTo(p2.getDocument())==0){
                    if (p1.getStart().compareTo(p2.getStart())==0) {
                        return p1.getEnd().compareTo(p2.getEnd());
                    }
                }
                return p1.getDocument().compareTo(p2.getDocument());
            }
        };
    }
}
