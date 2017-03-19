package com.istratenko.searcher.tokenizer;

/**
 * Created by denis on 16.03.17.
 */
public class WordItem {
    private String word;
    private int line;
    private int startSymbol;
    private int endSymbol;

    public WordItem(String word, int line, int startSymbol, int endSymbol){
        this.word=word;
        this.line=line;
        this.startSymbol=startSymbol;
        this.endSymbol=endSymbol;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(int startSymbol) {
        this.startSymbol = startSymbol;
    }

    public int getEndSymbol() {
        return endSymbol;
    }

    public void setEndSymbol(int endSymbol) {
        this.endSymbol = endSymbol;
    }
}
