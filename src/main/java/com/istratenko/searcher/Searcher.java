package com.istratenko.searcher;

import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.WordItem;
import com.istratenko.searcher.tokenizer.WordSearcher;


import java.io.IOException;
import java.util.*;

/**
 * Created by denis on 21.03.17.
 */
public class Searcher {

    public List<Positions> getPositionsInDocByQuery(final MongoDbWorker mdb, String queryWords) throws IOException {
        List<Positions> currentSet;
        List<Positions> resultSet = new ArrayList<>();
        WordSearcher wordSearcher = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator"))));
        List<WordItem> wordsFromQuery = wordSearcher.getWordsFromFile(null, lines);
        boolean firstStep = true;
        for (WordItem wi : wordsFromQuery) { //проходим по всем словам в запросе
            Map<String, List<Positions>> word = mdb.getIndex(wi.getWord()); //для каждого слова получаем инфомрацию из БД
            if (!word.isEmpty()) {
                if (firstStep) {
                    resultSet.addAll(word.get(wi.getWord())); //при первом прохождении сохраняем информацию о нем, как финальный набор
                }
                currentSet = new ArrayList<>(word.get(wi.getWord()));
                //делаем объединение текущего слова и финальной коллекции. При первом прохождении должен вернуть ту же самую коллекцию.
                //При втором- объединение первой и второй
                resultSet = intersection(currentSet, resultSet);
                firstStep = false;
            }
        }
        return resultSet; //возвращает набор позиций
    }


    private List<Positions> intersection(List<Positions> a, List<Positions> b) {
        Set<Positions> canAdd = new HashSet<>(a);
        List<Positions> result = new ArrayList<>();

        if (a.size() <= b.size()) {
            for (Positions n : b) {
                if (a.contains(n)) {
                    result.add(n);
                    canAdd.remove(n);
                }
            }
        } else {
            for (Positions n : a) {
                if (b.contains(n)) {
                    result.add(n);

                    canAdd.remove(n);
                }
            }
        }

        return result;
    }
/*
    public List<Positions> getPositionsInDocByQuery(final MongoDbWorker mdb, String queryWords) throws IOException {
        List<Positions> currentSet;
        List<Positions> resultSet = new ArrayList<>();
        WordSearcher wordSearcher = new WordSearcher();
        //List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator"))));
        //List<WordItem> wordsFromQuery = wordSearcher.getWordsFromFile(null, lines);
        boolean firstStep = true;
        //for (WordItem wi : wordsFromQuery) { //проходим по всем словам в запросе
            Map<String, List<Positions>> word = mdb.getIndex(queryWords); //для каждого слова получаем инфомрацию из БД
            if (!word.isEmpty()) {
                if (firstStep) {
                    resultSet.addAll(word.get(queryWords)); //при первом прохождении сохраняем информацию о нем, как финальный набор
                }
                currentSet = new ArrayList<>(word.get(queryWords));
                //делаем объединение текущего слова и финальной коллекции. При первом прохождении должен вернуть ту же самую коллекцию.
                //При втором- объединение первой и второй
                resultSet = intersection(currentSet, resultSet);
                firstStep = false;
            }
       // }
        return resultSet; //возвращает набор позиций
    }
*/
    //String-pathToFile
    //Integer[] - array of positions
    public Map<String, List<Positions>> getPositionOfWordsByDocuments(final MongoDbWorker mdb, String queryWords) throws IOException {
        Map<String, List<Positions>> documents = new HashMap<>();
        List<Positions> positions = getPositionsInDocByQuery(mdb,queryWords);
        for (Positions p : positions) {
            if (documents.get(p.getDocument())==null){
                List<Positions> pp = new ArrayList<Positions>();
                pp.add(p);
                documents.put(p.getDocument(), pp);
            } else {
                List<Positions> pp = new ArrayList<Positions>(documents.get(p.getDocument()));
                pp.add(p);
                documents.put(p.getDocument(), pp);
            }
        }
        return documents;
    }
}
