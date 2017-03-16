package com.searcher.wordsearcher.istratenko;import java.io.IOException;import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.nio.file.Paths;import java.util.ArrayList;import java.util.List;/** * Created by denis on 16.03.17. */public class WordSearcher {    private List<WordItem> getWordsFromText(String pathToTextFile) throws IOException {        int startSymbol = 0;        int currSymbol=0;        boolean isWord = false;        List<WordItem> listOfWords = new ArrayList<>();        List<String> lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document        int currLine = 0;        for (String line : lines) {            for (currSymbol = 0; currSymbol < line.length(); currSymbol++) {                if (Character.isLetter(line.charAt(currSymbol)) && (!isWord)) { //first step after not World (after space, first symbol in line etc.)                    startSymbol = currSymbol;                    isWord = true;                }                if (!Character.isLetter(line.charAt(currSymbol)) && (isWord)) { //if previous symbol was part of Word, but current symbol not letter (space, digit etc.)                    listOfWords.add(new WordItem(line.substring(startSymbol, currSymbol), currLine, startSymbol, currSymbol));                    isWord = false;                }            }            if (isWord && !line.isEmpty() && line.charAt(line.length() - 1) != '\n') { //if previous symbol was letter, but line ended and last symbol in line was not \n                listOfWords.add(new WordItem(line.substring(startSymbol, currSymbol), currLine, startSymbol, currSymbol));            }            currLine++;        }        return listOfWords;    }    /**     * printed words from text file     * @param pathToTextFile     * @throws IOException     */    public void printWordsFromText(String pathToTextFile) throws IOException {        for (WordItem wordItem : getWordsFromText(pathToTextFile)) {            System.out.println(new StringBuilder().                    append(wordItem.getWord()).                    append(", ").append(wordItem.getLine()).                    append(", ").append(wordItem.getStartSymbol()).                    append(", ").append(wordItem.getEndSymbol()).toString());        }    }}