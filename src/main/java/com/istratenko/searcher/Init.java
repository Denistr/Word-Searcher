package com.istratenko.searcher;

import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.Word;
import com.istratenko.searcher.tokenizer.WordSearcher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by denis on 16.03.17.
 */
public class Init {
    private static final MongoDbWorker mdb = MongoDbWorker.getInstance();
    private static InputStream  input = null;
    private static Properties configProp = new Properties();

    public static void main(String[] args) {
        try {

            new Init().init(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input!=null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
/*
    private void readFile(String pathToFile) throws IOException {
        FileInputStream inputStream = null;
        Scanner sc = null;
        try {
            inputStream = new FileInputStream(pathToFile);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                String line = sc.next();
                 System.out.print(line);
            }
            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (sc != null) {
                sc.close();
            }
        }
    }
*/


    //TODO:сделать защиту, если путь до файла не будет указан. Чтоб не получитьNPE
    private void init(String pathToConfigProp) throws IOException {
        input = new FileInputStream(pathToConfigProp); //path to config.properties
        configProp.load(input);
        String mode = configProp.getProperty("mode").toLowerCase();
        List<String> lines = null;
        //TODO:приводить все слова к нижнему регистру при записи в базу
        if (mode.equals("1") || mode.equals("tokenizer")) {
            String pathToTextFile = configProp.getProperty("pathToFile");
            lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            WordSearcher s = new WordSearcher();
            s.printWordsFromText(pathToTextFile, lines);
        }

        if (mode.equals("2") || mode.equals("indexer")) {
            String pathToTextFile = configProp.getProperty("pathToFile");
            String pathToMDBConf = configProp.getProperty("pathToMongoDbConfig");
            mdb.initConnection(pathToMDBConf);
            if (mdb.isAuthenticate()) {
                System.out.println("Connection is ok");
            } else {
                System.out.println("Connection refused");
            }
            lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            WordSearcher s = new WordSearcher();
            List<Word> words = s.getWords(true, pathToTextFile, lines);
            Map<String, List<Positions>> content = s.getAllPositionOfWord(words);
            mdb.addIndex(content);
        }

        if (mode.equals("3") || mode.equals("WordSearcher")) {
            String pathToMDBConf = configProp.getProperty("pathToMongoDbConfig");
            mdb.initConnection(pathToMDBConf);
            if (mdb.isAuthenticate()) {
                System.out.println("Connection is ok");
            } else {
                System.out.println("Connection refused");
            }
            Searcher searcher = new Searcher();
            System.out.println("Enter your search query:");
            String query = new Scanner(System.in).nextLine();
            Map<String, Map<String, List<Positions>>> findedDocuments = searcher.getPositionInDocument(mdb, query);
            for (Map.Entry position : findedDocuments.entrySet()) {
                List<Positions> allPositions = (List<Positions>) position.getValue();
                boolean firstStep = true;
                for (Positions p : allPositions) {
                    if (firstStep) {
                        System.out.println(p.getDocument() + "\n" + p.getLine() + ", " + p.getStart() + ", " + p.getEnd());
                        firstStep = false;
                    } else {
                        System.out.println(p.getLine() + ", " + p.getStart() + ", " + p.getEnd());
                    }
                }
            }
        }

        if (mode.equals("4") || mode.equals("WordSearcher")) {
            String pathToMDBConf = configProp.getProperty("pathToMongoDbConfig");
            mdb.initConnection(pathToMDBConf);
            if (mdb.isAuthenticate()) {
                System.out.println("Connection is ok");
            } else {
                System.out.println("Connection refused");
            }
            Searcher searcher = new Searcher();
            System.out.println("Enter your search query:");
            String query = new Scanner(System.in).nextLine();
            searcher.getSublineInDoc(mdb, query);
        }
    }
}
