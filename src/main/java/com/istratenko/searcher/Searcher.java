package com.istratenko.searcher;

import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.Word;
import com.istratenko.searcher.tokenizer.WordSearcher;


import java.io.*;
import java.util.*;

/**
 * Created by denis on 21.03.17.
 */
public class Searcher {

    /**
     *
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
        List<Word> wordsFromQuery = wordSearcher.getWords(null, lines);
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
        for (Word n : a) { //проходим по первой коллекции
            for (Word nn : b) {
                if (n.getPositions().getDocument().equals(nn.getPositions().getDocument())) {
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
        List<Word> positions = searchDocuments(mdb, queryWords);
        for (Word p : positions) {
            if (documents.get(p.getPositions().getDocument()) == null) {
                List<Positions> pp = new ArrayList<>();
                pp.add(p.getPositions());
                documents.put(p.getPositions().getDocument(), pp);
            } else {
                List<Positions> pp = new ArrayList<>(documents.get(p.getPositions().getDocument()));
                pp.add(p.getPositions());
                documents.put(p.getPositions().getDocument(), pp);
            }
        }
        return documents;
    }


    public void getSublineInDoc(final MongoDbWorker mdb, String queryWords) throws IOException {
        List<Word> wordsInText = searchDocuments(mdb, queryWords);
        WordSearcher wordSearcher = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator")))); //разбиваем запрос на отдельные слова
        List<Word> wordsFromQuery = wordSearcher.getWords(null, lines);
        Set<Word> possiblePhrase = getPossibleQueryPosInText(wordsInText,wordsFromQuery,queryWords); //получаю список фраз с их началами и концами в тексте
        //теперь имея координаты посибл фраз я ищу в тексте, делаю сабстринг и если фраза та, то я определяю границы предложения
        Set<Word> findedPhrase = defineEqualsPhraseInDoc(possiblePhrase);
        for (Word w : possiblePhrase){
            System.out.println(w.getWord()+", "+w.getPositions().getStart()+", "+w.getPositions().getEnd()+", "+w.getPositions().getLine());
        }


        /*for (Map.Entry document : documents.entrySet()) { //проходимся по каждому документу, в которых были найдены все эти слова
            //List<String> lines = Files.readAllLines(Paths.get(document.getKey().toString()), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            List<Positions> positions = (List<Positions>) document.getValue();
            for (Positions p : positions) { //проходимся по всем позициям в документе
                String line = lines.get(p.getLine()); //получаю строку, в которой было найдено слово
                if () //
            }

        }*/
    }


    private Set<Word> getPossibleQueryPosInText(List<Word> wordInText, List<Word> wordsInQuery, String queryWords) {
        Set<Word> posiblePosList = new HashSet<>();
        //WordItem possiblePhrase=null;
        for (Word wi : wordInText) { //цикл по всем найденным словам,  в одном документе
            for (Word word : wordsInQuery) { //для каждого слова в запросе
                Word possiblePhrase=new Word();
                if (wi.getWord().equals(word.getWord())) {
                    if (wordsInQuery.indexOf(word) == 0) {
                        //прибавляем длину строки без первого слова к конечной позиции
                        String document = wi.getPositions().getDocument();
                        Integer line = wi.getPositions().getLine();
                        Integer start = wi.getPositions().getStart();
                        Integer end = wi.getPositions().getEnd() + (queryWords.length() - word.getWord().length()-1);
                        possiblePhrase.setPositions(new Positions(document, line, start, end));
                        possiblePhrase.setWord(queryWords);
                    } else if (wordsInQuery.indexOf(word) == wordsInQuery.size()-1) { //возможно, size-1
                        //если последнее слово, то отнимаем у стартовой позиции
                        String document = wi.getPositions().getDocument();
                        Integer line = wi.getPositions().getLine();
                        Integer start = wi.getPositions().getStart() - (queryWords.length() - word.getWord().length()-1);
                        Integer end = wi.getPositions().getEnd();
                        possiblePhrase.setPositions(new Positions(document, line, start, end));
                        possiblePhrase.setWord(queryWords);
                    } else {
                        String document = wi.getPositions().getDocument();
                        Integer line = wi.getPositions().getLine();
                        Integer start = wi.getPositions().getStart() - (word.getPositions().getStart());
                        Integer end = wi.getPositions().getEnd() + (queryWords.length()-1-word.getPositions().getEnd()-1);

                        possiblePhrase.setPositions(new Positions(document, line, start, end));
                        possiblePhrase.setWord(queryWords);
                    }
                    //possiblePhrase.setWord(queryWords);

                    //потом делаем проверку: если позиции получившися воркайтемов равны, то нужно взять именно этот ворк айтем и его искать в документе
                    posiblePosList.add(possiblePhrase);
                }
                //На выходе получаем WI,где word-это вся строка, начальная позиция и конечная - новые вычисленные

            }
        }
        return posiblePosList;
    }

    private Set<Word> defineEqualsPhraseInDoc(Set<Word> possiblePhrase){
        Set<Word> phraseList = new HashSet<>();
        for (Word p : possiblePhrase) { //берем одно слово
            for (Word pp : possiblePhrase) { //проходимся по все словам и сравниваем p со всеми словами pp
                if (p.getPositions().equals(pp.getPositions())) {
                    phraseList.add(p);
                }
            }
        }
        return phraseList; //получаем коллекцию уникальных фраз в тексте
    }

}
