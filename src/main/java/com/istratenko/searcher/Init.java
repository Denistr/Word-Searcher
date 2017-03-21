package com.istratenko.searcher;

import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.WordItem;
import com.istratenko.searcher.tokenizer.WordSearcher;
import javafx.geometry.Pos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by denis on 16.03.17.
 */
public class Init {
    private static final MongoDbWorker mdb = MongoDbWorker.getInstance();

    public static void main(String[] args) {
        List<String> lines = null;
        Searcher searcher = new Searcher();
        //args[0] - path to Text File
        //args[1] - path to config.properties for Mongo db connection

        //читаю файл
        //индексирую его
        //ввожу в консоль текст

        String pathToTextFile = args[0];
        try {
            lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            WordSearcher s = new WordSearcher();
            //s.printWordsFromText(pathToTextFile, lines);
            mdb.initConnection(args[1]);
            System.out.println(mdb.isAuthenticate());
            List<WordItem> words = s.getWordsFromFile(pathToTextFile, lines);
            Map<String, List<Positions>> content = s.getAllPositionOfWord(words);
            mdb.addIndex(content);

            //Map<String, List<Positions>> findedDocuments = searcher.getPositionsInDocByQuery(mdb, new Scanner(System.in).nextLine());
            //:TODO добиться вывода позиций слова по документу чтобы один раз документ, а после него - все позиции
            List<Positions> findedDocuments = searcher.getPositionsInDocByQuery(mdb, new Scanner(System.in).nextLine());
            for (Positions position : findedDocuments) {
                    System.out.println(position.getDocument()+ position.getLine()+position.getStart()+ position.getEnd());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        try {
            //s.printWordsFromText(args[0]);
            List<WordItem> words = s.getWordsFromFile(pathToTextFile, lines);
            Map<String, List<Positions>> content = s.getAllPositionOfWord(words);
            mdb.addIndex(content);
            //mdb.getIndex(words.get(0).getWord());

        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}
