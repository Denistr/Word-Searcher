package com.istratenko.searcher.entity;

import com.mongodb.BasicDBObject;

import java.util.Comparator;

/**
 * Created by denis on 16.03.17.
 */
public class Word extends BasicDBObject implements Comparable<Word> {
    private String word;
    private Positions positions;

    public Word(){

    }
    public Word(String word, Positions positions){
        this.word=word;
        this.positions=positions;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Positions getPositions() {
        return positions;
    }

    public void setPositions(Positions positions) {
        this.positions = positions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Word word = (Word) o;

        return positions.equals(word.positions);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + positions.hashCode();
        return result;
    }


    @Override
    public int compareTo(Word w) {
        return Comparators.DOCUMENT.compare(this, w);
    }

    public static class Comparators {

        public static Comparator<Word> DOCUMENT = new Comparator<Word>() {
            @Override
            public int compare(Word w1, Word w2) {
               if (w1.getPositions().getDocument().compareTo(w2.getPositions().getDocument())==0){
                   return w1.getWord().compareTo(w2.getWord());
               }
                return w1.getPositions().getDocument().compareTo(w2.getPositions().getDocument());
            }
        };
        public static Comparator<Word> START = new Comparator<Word>() {
            @Override
            public int compare(Word w1, Word w2) {
                return w1.getPositions().getStart().compareTo(w2.getPositions().getStart());
            }
        };
    }

    @Override
    public String toString() {
        return "Word{" +
                "word='" + word + '\'' +
                ", positions=" + positions +
                '}';
    }
}
