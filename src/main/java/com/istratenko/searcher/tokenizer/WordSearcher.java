package com.istratenko.searcher.tokenizer;import com.istratenko.searcher.entity.Positions;import com.istratenko.searcher.entity.Word;import java.io.IOException;import java.util.ArrayList;import java.util.HashMap;import java.util.List;import java.util.Map;/** * Created by denis on 16.03.17. */public class WordSearcher {    public List<Word> getWords(String pathToTextFile, List<String> lines) throws IOException {        int startSymbol;        int currSymbol;        int numPrevSymbolsInText = 0;        boolean isWord = false;        List<Word> listOfWords = new ArrayList<>();        int currLine = 0;        boolean isLetter = false;        for (String line : lines) {            line = line.toLowerCase(); //приводим все символы к нижнему регистру, чтобы в дальнейшем обеспечить полноту выборки по запросу            startSymbol = 0;            for (currSymbol = startSymbol; currSymbol < line.length(); currSymbol++) {                isLetter = Character.isLetter(line.charAt(currSymbol));                if (isLetter && !isWord) { //first step after not world (after space, first symbol in line etc.)                    startSymbol = currSymbol;                    isWord = true;                }                if (!isLetter && isWord) { //if previous symbol was part of Word, but current symbol not letter (space, digit etc.)                    Positions positions = new Positions(pathToTextFile, currLine, startSymbol + numPrevSymbolsInText, currSymbol + numPrevSymbolsInText - 1); //not contains current symbol (space, \n etc.)                    listOfWords.add(new Word(line.substring(startSymbol, currSymbol), positions));                    isWord = false;                }            }            if (isWord && !line.isEmpty() && line.charAt(line.length() - 1) != '\n') { //if previous symbol was letter, but line ended and last symbol in line was not \n                Positions positions = new Positions(pathToTextFile, currLine, startSymbol + numPrevSymbolsInText, currSymbol + numPrevSymbolsInText - 1); //not consider current symbol (space, \n etc.)                listOfWords.add(new Word(line.substring(startSymbol, currSymbol), positions));                isWord = false;            }            currLine++;            numPrevSymbolsInText += line.length() + 1; //add length of line to checked symbols in text + /n        }        return listOfWords;    }    /**     * printed words from text file     *     * @param pathToTextFile     * @throws IOException     */    public void printWordsFromText(String pathToTextFile, List<String> lines) throws IOException {        for (Word word : getWords(pathToTextFile, lines)) {            System.out.println(new StringBuilder().                    append(word.getWord()).                    append(", ").append(word.getPositions().getLine()).                    append(", ").append(word.getPositions().getStart()).                    append(", ").append(word.getPositions().getEnd()).toString());        }    }    /**     * привожу данные к структуре ключ (слово) - значение - List позиций. Для записи в базу     * @param words список найденых слов     * @return Map ключ (слово) - значение - List позиций     */    public Map<String, List<Positions>> getAllPositionOfWord(List<Word> words) {        Map<String, List<Positions>> allPositions = new HashMap<>();        for (Word word : words) {            if (allPositions.get(word.getWord()) == null) {                //если такого слова в мапе нет, то добавляем                List<Positions> positions = new ArrayList<>();                positions.add(word.getPositions());                allPositions.put(word.getWord(), positions);            } else {                //если такое слово есть, то беру его из мапы                //делаю адд в уже существующий список                List<Positions> positions = new ArrayList<>(allPositions.get(word.getWord()));                positions.add(word.getPositions());                allPositions.put(word.getWord(), positions);            }        }        return allPositions;    }}