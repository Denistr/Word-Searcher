package com.istratenko.searcher.tokenizer;

import com.mongodb.BasicDBObject;

import java.io.Serializable;

/**
 * Created by denis on 16.03.17.
 */
public class WordItem extends BasicDBObject {
    private String word;
    private Positions positions;

    public WordItem(){

    }
    public WordItem(String word, Positions positions){
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
}
