package com.istratenko.searcher.entity;

import com.mongodb.BasicDBObject;

/**
 * Created by denis on 16.03.17.
 */
public class Word extends BasicDBObject {
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
}
