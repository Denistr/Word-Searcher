package com.istratenko.searcher;

import com.istratenko.searcher.entity.Phrase;
import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.Word;
import com.istratenko.searcher.tokenizer.WordSearcher;
import org.apache.commons.collections.CollectionUtils;


import java.io.*;
import java.util.*;
import java.util.concurrent.Phaser;

/**
 * Created by denis on 21.03.17.
 */
public class Searcher {

    /**
     * @param mdb
     * @param queryWords
     * @return Word Items List, для тех слов, которые встречаются в одном и том же документе
     * @throws IOException
     */
    private List<Word> searchDocuments(final MongoDbWorker mdb, String queryWords) throws IOException {
        List<Word> currentList;
        List<Word> resultList = new ArrayList<>();
        WordSearcher wordSearcher = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator"))));
        List<Word> wordsFromQuery = wordSearcher.getWords(true, null, lines);
        boolean firstStep = true;
        for (Word wi : wordsFromQuery) { //проходим по всем словам в запросе
            Map<String, List<Positions>> word = mdb.getIndex(wi.getWord()); //для каждого слова получаем инфомрацию из БД
            if (!word.isEmpty()) {
                if (firstStep) {
                    resultList.addAll(getListWordItemsFromMap(word)); //при первом прохождении сохраняем информацию о нем, как финальный набор
                }
                currentList = new ArrayList<>(getListWordItemsFromMap(word));
                //делаем объединение текущего слова и финальной коллекции. При первом прохождении должен вернуть ту же самую коллекцию.
                //При втором- объединение первой и второй
                if (!firstStep) {
                    resultList = (List<Word>) intersection(currentList, resultList);
                }
                firstStep = false;

                //СегодCollectionUtils.intersection()
            }
        }
        return resultList; //возвращает набор позиций
    }


    private List<Word> getListWordItemsFromMap(Map<String, List<Positions>> words) {
        List<Word> resultList = new ArrayList<>();
        for (Map.Entry word : words.entrySet()) {
            for (Positions p : (List<Positions>) word.getValue()) {
                Word wi = new Word((String) word.getKey(), p);
                resultList.add(wi);
            }
        }
        return resultList;
    }

    //TODO:проверить еще раз комбинациями 3-4 слова. Не работает!!! (когда одного из слов нету в базе - все равно выводит. А должен ли??)
    private Collection<Word> intersection(Collection<Word> a, Collection<Word> b) {
        Collection<Word> result = new ArrayList<>();
        List<Word> listToDelete = new ArrayList<>();
        for (Word n : a) { //проходим по первой коллекции
            for (Word nn : b) {
                if (n.getPositions().getDocument().equals(nn.getPositions().getDocument())) {
                    if (!listToDelete.contains(nn)) {
                        result.add(nn);
                    }
                    listToDelete.add(nn);
                    if (!result.contains(n)) {
                        result.add(n);
                    }
                }
            }
        }
        return result;
    }


    public List<Positions> getAllPositions(final MongoDbWorker mdb) throws IOException {
        Map<String, List<Positions>> allWords = mdb.getAllWords();
        List<Positions> allPositions = new ArrayList<>();
        for (Map.Entry word : allWords.entrySet()) {

            allPositions.addAll((List<Positions>) word.getValue());
        }
        Collections.sort(allPositions, Positions.Comparators.POSITIONS);
        return allPositions;
    }

    public Map<String, Map<String, List<Positions>>> getPositionInDocument(final MongoDbWorker mdb, String queryWords) throws IOException {
        Map<String, Map<String, List<Positions>>> documents = new HashMap<>(); //key - документ, key - слово

        List<Word> findedWordsList = searchDocuments(mdb, queryWords); //тут нужно возвращать лист WordItem, который содержаться в одном документе (средставми базы, а не java)

        Collections.sort(findedWordsList, Word.Comparators.DOCUMENT); //sort by documents

        String currDocument = null;
        String currWord = null;
        HashMap<String, List<Positions>> wordPositions = null;
        List<Positions> listPositions = null;
        for (Word findedWord : findedWordsList) {
            if (!findedWord.getPositions().getDocument().equals(currDocument)) {
                currWord = null;
                wordPositions = new HashMap<>();
                currDocument = findedWord.getPositions().getDocument();
                documents.putIfAbsent(currDocument, wordPositions);
            }

            if (!(findedWord.getWord().equals(currWord))) {
                currWord = findedWord.getWord();
                listPositions = new ArrayList<>();
                wordPositions.put(currWord, listPositions);
            }
            if (listPositions != null) {
                listPositions.add(findedWord.getPositions());
            }
        }
        return documents;
    }

    public void getSublineInDoc(final MongoDbWorker mdb, String queryWords) throws IOException {
        List<Word> wordsInText = searchDocuments(mdb, queryWords);
        WordSearcher wordSearcher = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator")))); //разбиваем запрос на отдельные слова
        List<Word> wordsFromQuery = wordSearcher.getWords(true, null, lines);
        List<Positions> pos=getAllPositions(mdb);
        for (Positions p : pos){
            System.out.println(p.getDocument() + "," + p.getStart());
        }
        //Set<Word> possiblePhrase = getPossibleQueryPosInText(wordsInText, wordsFromQuery, queryWords); //получаю список фраз с их началами и концами в тексте
        //теперь имея координаты посибл фраз я ищу в тексте, делаю сабстринг и если фраза та, то я определяю границы предложения
        //Set<Word> findedPhrase = defineEqualsPhraseInDoc(possiblePhrase);
        /*List<Phrase> a= getIntersectionPhrase(getPossibleQueryPosInText(wordsInText, wordsFromQuery, queryWords));
        for (Phrase w : a) {
            System.out.println(w.getStart() + ", " + w.getEnd());
        }*/
    }
}
