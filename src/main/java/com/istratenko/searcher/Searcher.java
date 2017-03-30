package com.istratenko.searcher;

import com.istratenko.searcher.entity.CtxWindow;
import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.Word;
import com.istratenko.searcher.tokenizer.WordSearcher;
import com.sun.deploy.util.StringUtils;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Set<CtxWindow> identifyContextPosition(List<Positions> allWordsInDB, Map<String, List<Positions>> word, int contextWindow) throws IOException {
        List<CtxWindow> contextWindowPos;
        Set<CtxWindow> resultWindow;
        List<List<CtxWindow>> commonlist = new ArrayList<>();
        for (Map.Entry position : word.entrySet()) {
            String currWord = (String) position.getKey();
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
                //CtxWindow ctxWindow = trimmingSentence(p.getDocument(), ctxStart, ctxEnd, currWord);
                contextWindowPos.add(ctxWindow);


            }
            commonlist.add(contextWindowPos);
            //получаю результирующий спиок цитат в одном документе
            //мержу цитаты для одного документа по каждому слову
            //возвращаю список смерженных цитат
        }
        resultWindow = new HashSet<>(ctxAssociation(commonlist));

        return resultWindow;
    }


    private CtxWindow trimmingSentence(String document, int start, int end, String word) throws IOException { //этот метод не учитывает N количество пробелов и N
        // количество знаков припенания между предлложениями->неправильно опеределются позици начала и конца
        String text = new String(Files.readAllBytes(Paths.get(document)));
        int newStart = start;
        int newEnd = end;
        String sentenceArray[] = text.substring(start, end + 1).split("[!|\\.|\\?]+[\\s]+"); //regexp, который делит контекст на предложения по .?! и пробелам
        text = null; //чтобы gc очистил память от текста
        for (int i = 0; i < sentenceArray.length; i++) {
            if (sentenceArray[i].contains(word)) { //если в найденном предложении содержится слово, то беру его и определяю его новые координаты в тексте
                if (i == 0) { //если это первое преложение из контекста
                    newStart = start;
                    newEnd = start + sentenceArray[i].length() - 1;
                } else if (i == sentenceArray.length - 1) { //если это последнее предложение в тексте
                    newStart = end - sentenceArray[i].length() + 1;
                    newEnd = end;
                } else { //если это предложение в середине
                    int summLeng = 0;
                    for (int j = 0; j < i; j++) { //цикл по всем предложениям до текущего i, чтобы найти их суммарную длину
                        summLeng += sentenceArray[j].length() + 2;
                    }
                    newStart = start + summLeng;
                    newEnd = newStart + sentenceArray[i].length() - 1; //стартовая позиция найденного фрагмента+длина найденного фрагмента; минус точка и пробел
                }
            }
        }
        return new CtxWindow(document, newStart, newEnd);
    }

    /**
     * Объединяет контексты слов
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
        CtxWindow ctx = null;
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
            resultContextWindow.removeAll(deleteList);
        }

        return resultContextWindow;
    }




    private Map<String, List<String>> selectWordsInPhrase(Map<String, List<String>> phrases, List<Word> wordsFromQuery) throws IOException { //уже отсортированных по возрастанию
        Map<String, List<String>> documentPhrases = new HashMap<>();

        List<String> words = new ArrayList<>();
        for (Word w : wordsFromQuery) {
            words.add(w.getWord());
        }

        //String patternString = "\\b([0-9]*" + StringUtils.join(words, "|") + "[0-9]*)\\b";
        //Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        String currDocument = null;
        List<String> resultPhrases = null;
        for (Map.Entry document : phrases.entrySet()) {
            if (!document.getKey().equals(currDocument)) {
                currDocument = (String) document.getKey();
                resultPhrases = new ArrayList<>();
                documentPhrases.put((String) document.getKey(), resultPhrases);
            }

            List<String> phrase = (List<String>) document.getValue();
            WordSearcher ws = new WordSearcher();
            List<Word> clearMatchedWord = new ArrayList<>();

            for (String p : phrase) {
                boolean wasReplaces=false;
                List<String> matchedWords = new ArrayList<>();
                matchedWords.add(p);
                StringBuilder wordWithFormat = new StringBuilder();
                clearMatchedWord = ws.getWords(null, matchedWords);

                StringBuilder resultStr = new StringBuilder();
                String origp = p;
                int prevStart = 0;
                for (Word clearWord : clearMatchedWord) {

                    int wStart = clearWord.getPositions().getStart() - prevStart;
                    int wEnd   = clearWord.getPositions().getEnd() - prevStart;
                    int predCharPos = wStart - 1;
                    int postCharPos = wEnd + 1;

                    boolean isPredChar = true;
                    if (predCharPos>=0) {
                        isPredChar = !Character.isLetter(p.charAt(predCharPos));
                    }
                    boolean isPostChar = true;
                    if (postCharPos<p.length()) {
                        isPostChar = !Character.isLetter(p.charAt(postCharPos));
                    }
                    boolean       bWasBolded = false;
                    if(isPredChar && isPostChar) {
                    // Убедились что это отдельно стоящее слово или слово обрамленное цифрами
                        String foundword =  p.substring(wStart, wEnd+1);
                        if ( isWordInQuery(words, foundword )) {
                            resultStr.append(p.substring(0, wStart));
                            resultStr.append("<b>");
                            resultStr.append(foundword);
                            resultStr.append("</b>");
                            bWasBolded = true;
                        }
                    }
                    if(!bWasBolded)    {
                        resultStr.append(p.substring(0, wEnd+1));
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

    private boolean isWordInQuery( List<String> words, String word ){

        for (String w : words) {
            if (w.equalsIgnoreCase(word)) {
                return true;
            }
        }
        return false;
    }
    /**
     * расширяет границы найденного контекстного окна до границ предложений, в которые это контекстное окно входит
     *
     * @param contextList отсортированный по документам список позиций контекстов
     * @return Map: key - документ, value - List<String> список фраз из этого документа
     * @throws IOException
     */
    private Map<String, List<String>> increaseContextBorder(List<CtxWindow> contextList) throws IOException {
        String text = null;
        String currDocument = null;
        Map<String, List<String>> resultMap = new HashMap<>();
        List<String> phrase = null;
        for (CtxWindow ctx : contextList) {
            int newStart = 0;//ctx.getStart();
            int newEnd = 0;//ctx.getEnd();
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
                phrase = new ArrayList<>();
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

                if ((i == text.length() - 1 && isEndSent) || ((startSent > whitespaces && whitespaces > endSent) && (endSent != 0))) { //если пробел лежит между концом предыдущего и началом текущего
                    newEnd = endSent;
                    phrase.add(text.substring(newStart, newEnd + 1));
                    break;
                }
            }
            if (ctx.getStart().equals(newStart) && ctx.getEnd().equals(newEnd)) { //если позиции равны, то в циклы мы не заходили, то и берем этот кусок
                phrase.add(text.substring(ctx.getStart(), ctx.getEnd() + 1));
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
        Map<String, List<String>> documentPhrase = new HashMap<>();
        Map<String, List<String>> finalPhrases;
        WordSearcher ws = new WordSearcher();
        List<String> lines = new ArrayList<>(Arrays.asList(queryWords.split(System.getProperty("line.separator"))));
        List<Word> wordsFromQuery = ws.getWords(null, lines);
        HTMLController htmlCreator = new HTMLController();

        Map<String, Map<String, List<Positions>>> positionsWordsInDoc = getPositionInDocument(mdb, queryWords);
        if (positionsWordsInDoc.isEmpty()) {
            System.out.println("Nothing found on your request");
            return;
        }
        List<Positions> allWordsInDB = getAllPositions(mdb);
        for (Map.Entry word : positionsWordsInDoc.entrySet()) {
            List<CtxWindow> phraseiList = new ArrayList<>(identifyContextPosition(allWordsInDB, (Map<String, List<Positions>>) word.getValue(), sizeContext));
            if (phraseiList.isEmpty()) {
                System.out.println("This words have no context intersection");
                return;
            }
            Collections.sort(phraseiList, CtxWindow.Comparators.DOCUMENTS);
            for (CtxWindow pp : phraseiList) {
                System.out.println(pp.getDocument() + ", " + pp.getStart() + ", " + pp.getEnd());
            }
            documentPhrase.putAll(increaseContextBorder(phraseiList)); //добавляем а мапу уже увеличенный контекст до границы предложения
        }
        finalPhrases = selectWordsInPhrase(documentPhrase, wordsFromQuery); //выделяем жирным искомые слова во фразе
        htmlCreator.createHTMLFile(finalPhrases); //создаем html файл

    }
}
