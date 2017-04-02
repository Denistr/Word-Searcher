package com.istratenko.searcher;

import com.istratenko.searcher.entity.CtxWindow;
import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.Word;
import com.istratenko.searcher.tokenizer.WordSearcher;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        List<Word> wordsFromQuery = wordSearcher.getWords(null, lines);
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
        return resultList; //возвращает набор позиций слов из одного документа
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
     * Пересечение 2-ух ArrayList, содержащих объекты WorkItem.
     *
     * @param a первая коллекция
     * @param b вторая коллекция
     * @return ArrayList слов, содержащихся в одних документах
     */
    private Collection<Word> intersection(Collection<Word> a, Collection<Word> b) {
        Collection<Word> result = new ArrayList<>();
        List<Word> listToDelete = new ArrayList<>();
        for (Word n : a) { //проходим по первой коллекции
            for (Word nn : b) { //проходим по всем словам второй кллекции
                if (n.getPositions().getDocument().equals(nn.getPositions().getDocument())) { //если слова находятся в одинаковых документах
                    if (!listToDelete.contains(nn)) { //и оюъект из коллекции b еще не был добавлен в результирующую колекцию
                        result.add(nn);
                    }
                    listToDelete.add(nn); //запоминаем, что этот объект уже есть в коллекции
                    if (!result.contains(n)) { //если объект из коллекции a еще не был добавлен в результирующую колекцию
                        result.add(n);
                    }
                }
            }
        }
        return result;
    }


    /**
     * Возвращает из базы все слова, отсортированные по позициям в документе
     *
     * @param mdb объект класса MongoDbWorker для работы с БД
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
        Collections.sort(allPositions, Positions.Comparators.POSITIONS); //сортируем сначала по документам, потом по позициям в них
        return allPositions;
    }

    /**
     * Возвращает мапу всех позиций слова в документе
     *
     * @param mdb
     * @param queryWords строка поискового запроса
     * @return Map key=документ, Value=мап. Вложенный мап = key - слово, value=лист позиций этого слова
     * @throws IOException
     */
    public Map<String, Map<String, List<Positions>>> getPositionInDocument(final MongoDbWorker mdb, String queryWords) throws IOException {
        Map<String, Map<String, List<Positions>>> documents = new HashMap<>(); //key - документ, key - слово

        List<Word> findedWordsList = searchDocuments(mdb, queryWords); //получаю слова, которые находятся в одинаковымх документах
        if (findedWordsList == null) {
            return documents;
        }
        Collections.sort(findedWordsList, Word.Comparators.DOCUMENT); //сортируем все слова по документам, потом по словам

        String currDocument = null;
        String currWord = null;
        HashMap<String, List<Positions>> wordPositions = null;
        List<Positions> listPositions = null;
        for (Word findedWord : findedWordsList) {
            if (!findedWord.getPositions().getDocument().equals(currDocument)) { //если перешли к новому документу, то записывам все слова и позиции по старому
                currWord = null;
                wordPositions = new HashMap<>();
                currDocument = findedWord.getPositions().getDocument();
                documents.putIfAbsent(currDocument, wordPositions);
            }

            if (!(findedWord.getWord().equals(currWord))) { //попадаем сюда, если обрабатываем новое слово
                currWord = findedWord.getWord();
                listPositions = new ArrayList<>();
                wordPositions.put(currWord, listPositions);
            }
            if (listPositions != null) {
                listPositions.add(findedWord.getPositions()); //попадаем сюда, если мы обрабатываем все то же слово, но другую его позицию
            }
        }
        return documents;
    }


    /**
     * Определяем контекстное окно для найденного слова в тексте
     *
     * @param allWordsInDB  список всех слов в базе данных
     * @param word          мапа -ключ- слово, значение - лист его позиций в документе
     * @param contextWindow размер контекстного окна
     * @return лист листов, где i элемент списка - это слово из запроса, а лист i эл-та - это все контексты для этого слова
     * @throws IOException
     */
    private Set<CtxWindow> identifyContextPosition(List<Positions> allWordsInDB, Map<String, List<Positions>> word, int contextWindow) throws IOException {
        List<CtxWindow> contextWindowPos;
        Set<CtxWindow> resultWindow;
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
                    startI = 0;
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
        resultWindow = new HashSet<>(ctxAssociation(commonlist)); //объединяю контексты слов
        return resultWindow;
    }

    /**
     * Объединяет пересекающиеся контексты слов
     *
     * @param rawContext список контекстов каждого запрашиваемого слова
     * @return список пересекающихся контекстов по словам
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

                for (CtxWindow pred : resultContextWindow) {//проходимся по каждой позиции предыдущего слова
                    boolean bPredWorked = false;
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
            resultContextWindow.removeAll(deleteList); //удаляем все контексты, которые не пересеклись
        }

        return resultContextWindow;
    }


    /**
     * Выделяет искомые слова из поискового запроса жирным цветом
     *
     * @param phrases        - Map<String, Set<String>>, где ключ - это дкумент, значение - Set цитат, которые уже достали из текста
     * @param wordsFromQuery список слов из поискового запроса
     * @return Map<String, List<String>> ключ - документ, значение - список цитат из текста, искомые слова в которых выделены жирным
     * @throws IOException
     */
    private Map<String, List<String>> selectWordsInPhrase(Map<String, Set<String>> phrases, List<Word> wordsFromQuery) throws IOException {
        Map<String, List<String>> documentPhrases = new HashMap<>();

        List<String> words = new ArrayList<>();
        for (Word w : wordsFromQuery) {
            words.add(w.getWord());
        }

        String currDocument = null;
        List<String> resultPhrases = null;
        for (Map.Entry document : phrases.entrySet()) {
            if (!document.getKey().equals(currDocument)) {
                currDocument = (String) document.getKey();
                resultPhrases = new ArrayList<>();
                documentPhrases.put((String) document.getKey(), resultPhrases);
            }

            Set<String> phrase = (Set<String>) document.getValue();
            WordSearcher ws = new WordSearcher();
            List<Word> clearMatchedWord = new ArrayList<>();

            for (String p : phrase) {
                List<String> matchedWords = new ArrayList<>();
                matchedWords.add(p);
                clearMatchedWord = ws.getWords(null, matchedWords);

                StringBuilder resultStr = new StringBuilder();
                String origp = p;
                int prevStart = 0;
                for (Word clearWord : clearMatchedWord) {

                    int wStart = clearWord.getPositions().getStart() - prevStart; //позиция начала слова относительно нового старта
                    int wEnd = clearWord.getPositions().getEnd() - prevStart; //позиция конца слова относительно нового старта
                    int predCharPos = wStart - 1;
                    int postCharPos = wEnd + 1;

                    boolean isPredChar = true;
                    if (predCharPos >= 0) {
                        isPredChar = !Character.isLetter(p.charAt(predCharPos));
                    }
                    boolean isPostChar = true;
                    if (postCharPos < p.length()) {
                        isPostChar = !Character.isLetter(p.charAt(postCharPos));
                    }
                    boolean bWasBolded = false;
                    if (isPredChar && isPostChar) {
                        // Убедились что это отдельно стоящее слово или слово обрамленное цифрами
                        String foundword = p.substring(wStart, wEnd + 1);
                        if (isWordInQuery(words, foundword)) { //если это слово из запроса, то выделяем его жирным
                            resultStr.append(p.substring(0, wStart))
                            .append("<b>")
                            .append(foundword)
                            .append("</b>");
                            bWasBolded = true;
                        }
                    }
                    if (!bWasBolded) {
                        resultStr.append(p.substring(0, wEnd + 1));
                    }
                    prevStart = prevStart + wEnd + 1;
                    p = origp.substring(prevStart);

                }
                resultStr.append(origp.substring(prevStart));
                resultPhrases.add(resultStr.toString());
            }
        }
        return documentPhrases;
    }

    /**
     * @param words список слов из запроса
     * @param word  слово, которое нжно проверить
     * @return true, если word соответвствует слову из запроса, иначе- false;
     */
    private boolean isWordInQuery(List<String> words, String word) {

        for (String w : words) {
            if (w.equalsIgnoreCase(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * расширяет границы найденного контекстного окна до границ предложений, в которые это контекстное окно входит, и достает из текста цитату
     *
     * @param contextList отсортированный по документам список позиций контекстов
     * @return Map: key - документ, value - Set<String> список фраз из этого документа
     * @throws IOException
     */
    private Map<String, Set<String>> increaseContextBorder(List<CtxWindow> contextList) throws IOException {
        String text = null;
        String currDocument = null;
        Map<String, Set<String>> resultMap = new HashMap<>();
        Set<String> phrase = null;
        for (CtxWindow ctx : contextList) {
            int newStart = 0;
            int newEnd = 0;
            int startSent = 0;
            int whitespaces = 0;
            int endSent = 0;

            if (!ctx.getDocument().equals(currDocument)) {

                boolean isExistsFile = new File(ctx.getDocument()).exists();

                if (!isExistsFile) {
                    System.out.println("File for with path is not found. Che it in config file");
                    return resultMap;
                }

                text = new String(Files.readAllBytes(Paths.get(ctx.getDocument())));
                phrase = new HashSet<>();
                currDocument = ctx.getDocument();
                resultMap.put(ctx.getDocument(), phrase);
            }

            for (int i = ctx.getStart(); i >= 0; i--) { //ищем левую границу предложения
                boolean isUpperLetter = Character.isLetter(text.charAt(i)) && Character.isUpperCase(text.charAt(i));
                if (isUpperLetter) {
                    startSent = i;
                }

                if (Character.isWhitespace(text.charAt(i))) {
                    whitespaces = i;
                }

                boolean isEndSent = text.charAt(i) == ('.') || text.charAt(i) == ('!') || text.charAt(i) == ('?');
                if (isEndSent) {
                    endSent = i;
                }

                if (endSent != 0 && (endSent < whitespaces && whitespaces < startSent)) { //если пробел лежит между концом предыдущего и началом текущего
                    newStart = startSent;
                    startSent = 0;
                    whitespaces = 0;
                    endSent = 0;
                    break;
                }
            }

            for (int i = ctx.getEnd(); i < text.length(); i++) { //ищем правую границу предложения
                boolean isUpperLetter = Character.isLetter(text.charAt(i)) && Character.isUpperCase(text.charAt(i));
                if (isUpperLetter) {
                    startSent = i;
                }

                if (Character.isWhitespace(text.charAt(i))) {
                    whitespaces = i;
                }

                boolean isEndSent = (text.charAt(i) == '.' || text.charAt(i) == ('!') || text.charAt(i) == ('?'));
                if (isEndSent) {
                    endSent = i;
                }

                if ((startSent > whitespaces && whitespaces > endSent) && (endSent != 0)) {  // если пробел лежит между концом предыдущего и началом текущего
                    newEnd = endSent;
                    phrase.add(text.substring(newStart, newEnd + 1));
                    break;
                } else if ((i == text.length() - 1)) { //если последний символ текста, т.к максимум можно расширить до конца текста
                    newEnd = i;
                    phrase.add(text.substring(newStart, newEnd + 1));
                    break;
                }
            }
        }

        return resultMap;
    }

    /**
     * Находит цитату по запрашиваемым словам в тексте и генерирует html файл
     *
     * @param mdb         инстанс класса MongoDbWorker
     * @param queryWords  поисковый запрос
     * @param sizeContext размер контекстного окна
     * @throws IOException
     */
    public void findQuoteInText(final MongoDbWorker mdb, String queryWords, int sizeContext) throws IOException {
        Map<String, Set<String>> documentPhrase = new HashMap<>();
        Map<String, List<String>> finalPhrases;
        WordSearcher ws = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator"))));
        List<Word> wordsFromQuery = ws.getWords(null, lines);
        HTMLController htmlCreator = new HTMLController();

        Map<String, Map<String, List<Positions>>> positionsWordsInDoc = getPositionInDocument(mdb, queryWords); //получаем позиции слов, которые находятся в одном документе
        if (positionsWordsInDoc.isEmpty()) {
            System.out.println("Nothing found on your request");
            return;
        }
        List<Positions> allWordsInDB = getAllPositions(mdb);
        for (Map.Entry word : positionsWordsInDoc.entrySet()) {
            List<CtxWindow> phraseiList = new ArrayList<>(identifyContextPosition(allWordsInDB, (Map<String, List<Positions>>) word.getValue(), sizeContext)); //определяем границы контекстного окна
            if (phraseiList.isEmpty()) {
                System.out.println("This words have no context intersection");
                return;
            }
            Collections.sort(phraseiList, CtxWindow.Comparators.DOCUMENTS);
            /*for (CtxWindow pp : phraseiList) {
                System.out.println(pp.getDocument() + ", " + pp.getStart() + ", " + pp.getEnd());
            }*/
            documentPhrase.putAll(increaseContextBorder(phraseiList)); //увеличиваем контекст до границы предложений и  добавляем а мапу уже увеличенный контекст
        }
        finalPhrases = selectWordsInPhrase(documentPhrase, wordsFromQuery); //выделяем жирным искомые слова во фразе
        htmlCreator.createHTMLFile(finalPhrases); //создаем html файл

    }
}
