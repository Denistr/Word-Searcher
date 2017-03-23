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

    private List<Positions> searchDocuments(final MongoDbWorker mdb, String queryWords) throws IOException {
        List<Positions> currentList;
        List<Positions> resultList = new ArrayList<>();
        WordSearcher wordSearcher = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator"))));
        List<WordItem> wordsFromQuery = wordSearcher.getWordsFromFile(null, lines);
        boolean firstStep = true;
        for (WordItem wi : wordsFromQuery) { //проходим по всем словам в запросе
            Map<String, List<Positions>> word = mdb.getIndex(wi.getWord()); //для каждого слова получаем инфомрацию из БД
            if (!word.isEmpty()) {
                if (firstStep) {
                    resultList.addAll(word.get(wi.getWord())); //при первом прохождении сохраняем информацию о нем, как финальный набор
                }
                currentList = new ArrayList<>(word.get(wi.getWord()));
                //делаем объединение текущего слова и финальной коллекции. При первом прохождении должен вернуть ту же самую коллекцию.
                //При втором- объединение первой и второй
                if (!firstStep) {
                    resultList = (List<Positions>)intersection(currentList, resultList);
                }
                firstStep = false;
            }
        }
        return resultList; //возвращает набор позиций
    }

    //TODO:проверить еще раз комбинациями 3-4 слова. Не работает!!! (когда одного из слов нету в базе - все равно выводит. А должен ли??)
    private Collection<Positions> intersection(Collection<Positions> a, Collection<Positions> b) {
        Collection<Positions> result = new ArrayList<>();
        for (Positions n : a) { //проходим по первой коллекции
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
        List<Positions> positions = searchDocuments(mdb, queryWords);
        for (Positions p : positions) {
            if (documents.get(p.getDocument()) == null) {
                List<Positions> pp = new ArrayList<>();
                pp.add(p);
                documents.put(p.getDocument(), pp);
            } else {
                List<Positions> pp = new ArrayList<>(documents.get(p.getDocument()));
                pp.add(p);
                documents.put(p.getDocument(), pp);
            }
        }
        return documents;
    }

}
