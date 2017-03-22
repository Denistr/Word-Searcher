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

    private Set<Positions> searchDocuments(final MongoDbWorker mdb, String queryWords) throws IOException {
        Set<Positions> currentSet;
        Set<Positions> resultSet = new HashSet<>();
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
                currentSet = new HashSet<>(word.get(wi.getWord()));
                //делаем объединение текущего слова и финальной коллекции. При первом прохождении должен вернуть ту же самую коллекцию.
                //При втором- объединение первой и второй
                if (!firstStep) {
                    resultSet = intersection(currentSet, resultSet);
                }
                firstStep = false;
            }
        }
        return resultSet; //возвращает набор позиций
    }


    private Set<Positions> intersection(Set<Positions> a, Set<Positions> b) {
        Set<Positions> result = new HashSet<>();
        for (Positions n : a) {
            for (Positions nn : b) {
                if (n.getDocument().equals(nn.getDocument())) {
                    result.add(nn);
                    if (!result.contains(n)) {
                        result.add(n);
                    }
                }
            }
        }
        return result;
    }


    public Map<String, List<Positions>> getPositionInDocument(final MongoDbWorker mdb, String queryWords) throws IOException {
        Map<String, List<Positions>> documents = new HashMap<>();
        Set<Positions> positions = searchDocuments(mdb, queryWords);
        for (Positions p : positions) {
            if (documents.get(p.getDocument()) == null) {
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
