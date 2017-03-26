package com.istratenko.searcher;

import com.istratenko.searcher.entity.CtxWindow;
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
     * Парсит входной поисковый запрос по словам, находит каждое слово в базе. Если такое словоне найдено, то возвращается null.
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
        List<Word> wordsFromQuery = wordSearcher.getWords(true, null, lines);
        boolean firstStep = true;
        for (Word wi : wordsFromQuery) { //проходим по всем словам в запросе
            Map<String, List<Positions>> word = mdb.getIndex(wi.getWord()); //для каждого слова получаем инфомрацию из БД
            if (word.isEmpty()) {
                return null;//если в базе нет запрашиваемого слова
            }
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
        return resultList; //возвращает набор позиций
    }


    /**
     * трансформирует мапу слово-массив позиций в лист объектов WordItem
     *
     * @param words
     * @return
     */
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

    /**
     * пересечение 2-ух ArrayList, содержащих объекты WorkItem.
     *
     * @param a
     * @param b
     * @return ArrayList слов, содержащихся в одних документах
     */
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


    /**
     * @param mdb
     * @return список позиций всех слов, содержащихся в базе, отсортированный по позициям
     * сортировка происходит сначала по документу, потом по страторов позиции слова
     * @throws IOException
     */
    public List<Positions> getAllPositions(final MongoDbWorker mdb) throws IOException {
        Map<String, List<Positions>> allWords = mdb.getAllWords();
        List<Positions> allPositions = new ArrayList<>();
        for (Map.Entry word : allWords.entrySet()) {

            allPositions.addAll((List<Positions>) word.getValue());
        }
        Collections.sort(allPositions, Positions.Comparators.POSITIONS);
        return allPositions;
    }

    /**
     * @param mdb
     * @param queryWords
     * @return Map key=документ, Value=мап. Вложенный мап = key - слово, value=лист позиций этого слова
     * @throws IOException
     */
    public Map<String, Map<String, List<Positions>>> getPositionInDocument(final MongoDbWorker mdb, String queryWords) throws IOException {
        Map<String, Map<String, List<Positions>>> documents = new HashMap<>(); //key - документ, key - слово

        List<Word> findedWordsList = searchDocuments(mdb, queryWords);
        if (findedWordsList == null) {
            return documents;
        }
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


    /**
     * @param allWordsInDB
     * @param word
     * @param contextWindow
     * @return лист листов, где i элемент списка - это слово из запроса, а лист - это все контексты для этого слова
     * @throws IOException
     */
    private List<CtxWindow> identifyContextPosition(List<Positions> allWordsInDB, Map<String, List<Positions>> word, int contextWindow) throws IOException {
        List<CtxWindow> contextWindowPos;
        List<CtxWindow> resultWindow = null;
        List<List<CtxWindow>> commonlist = new ArrayList<>();
        for (Map.Entry position : word.entrySet()) {
            contextWindowPos = new ArrayList<>();
            //беру текущее слово, нахожу его в листе всех слов, определяю его индекс в отсортированном списке

            for (Positions p : (List<Positions>) position.getValue()) {
                int currWordPositionInDB = allWordsInDB.indexOf(p);

                Positions startWordInContext = null;
                Positions endWordInContext = null;
                int startI;
                int endI;

                if (currWordPositionInDB - contextWindow <= 0) { //если разность с контекстным окном<=0, то берем первое слово в списке
                    startI = currWordPositionInDB;
                } else {
                    startI = currWordPositionInDB - contextWindow;
                }

                if (currWordPositionInDB + contextWindow >= allWordsInDB.size()) { //если сумма вместе с контекстным окном выходит за пределы всех слов, то берем последнее слово
                    endI = allWordsInDB.size() - 1;
                } else {
                    endI = currWordPositionInDB + contextWindow;
                }

                for (int i = startI; i <= endI; i++) {
                    if (allWordsInDB.get(i).getDocument().equals(p.getDocument())) { //если i докмент текущему
                        if (startWordInContext == null) { //если стартовая позиция контекста еще не определена или равна текущему слову
                            startWordInContext = allWordsInDB.get(i);
                        }
                        endWordInContext = allWordsInDB.get(i);
                    }
                }

                int ctxStart = startWordInContext.getStart();
                int ctxEnd = endWordInContext.getEnd();
                CtxWindow ctxWindow = new CtxWindow(p.getDocument(), ctxStart, ctxEnd);
                contextWindowPos.add(ctxWindow);


            }
            commonlist.add(contextWindowPos);
            //получаю результирующий спиок цитат в одном документе
            //мержу цитаты для одного документа по каждому слову
            //возвращаю список смерженных цитат
        }
        resultWindow = ctxAssociation(commonlist);

        return resultWindow;
    }

    /**
     * @param rawContext
     * @return лист пересекающихся контекстов по словам
     */
    private List<CtxWindow> ctxAssociation(List<List<CtxWindow>> rawContext) {
        List<CtxWindow> resultContextWindow = new ArrayList<>();
        List<CtxWindow> deleteList = new ArrayList<>();


        int curS = 0;
        int curE = 0;
        int predS = 0;
        int predE = 0;

        resultContextWindow.addAll(rawContext.get(0)); //добавляем все контексты первого слова в результирующий список
        if (rawContext.size() > 1) {
            for (int i = 1; i < rawContext.size(); i++) { //  ищем пересекающиеся окна начиная со второго слова
                List<CtxWindow> currCtx = rawContext.get(i);
                Collections.sort(currCtx, CtxWindow.Comparators.POSITIONS);
                boolean bPredWorked = false;
                for (CtxWindow pred : resultContextWindow) {//проходимся по каждой позиции предыдущего слова
                    for (CtxWindow cur : currCtx) {//проходимся по каждой позиции для текущего слова

                        curS = cur.getStart().intValue();
                        predE = pred.getEnd().intValue();
                        curE = cur.getEnd().intValue();
                        predS = pred.getStart().intValue();

                        if ((curS < predE && curE > predS)) {
                            pred.setStart(Math.min(curS, predS)); // расширяем границы окна слова
                            pred.setEnd(Math.max(curE, predE));
                            bPredWorked = true;
                        }
                    }
                    if (!bPredWorked) {
                        deleteList.add(pred);
                    }
                }
            }
            resultContextWindow.removeAll(deleteList);
        }

        return resultContextWindow;
    }

    public void getSublineInDoc(final MongoDbWorker mdb, String queryWords) throws IOException {
        List<Word> wordsInText = searchDocuments(mdb, queryWords);
        WordSearcher wordSearcher = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator")))); //разбиваем запрос на отдельные слова
        //List<Word> wordsFromQuery = wordSearcher.getWords(true, null, lines);
        List<Positions> pos = getAllPositions(mdb);
        for (Positions p : pos) {
            System.out.println(p.getDocument() + "," + p.getStart() + "\n");
        }

        Map<String, Map<String, List<Positions>>> positionsWordsInDoc = getPositionInDocument(mdb, queryWords);
        List<Positions> allWordsInDB = getAllPositions(mdb);
        for (Map.Entry word : positionsWordsInDoc.entrySet()) {
            List<CtxWindow> p = identifyContextPosition(allWordsInDB, (Map<String, List<Positions>>) word.getValue(), 6);
            for (CtxWindow pp : p) {
                System.out.println(pp.getDocument() + ", " + pp.getStart() + ", " + pp.getEnd());
            }
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
