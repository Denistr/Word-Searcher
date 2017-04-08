package com.istratenko.searcher;

import com.istratenko.searcher.entity.Positions;
import com.istratenko.searcher.entity.Word;
import com.istratenko.searcher.tokenizer.WordSearcher;

import java.io.File;
import java.io.FileInputStream;
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
    private static InputStream input = null;
    private static Properties configProp = new Properties();

    public static void main(String[] args) {
        try {

            new Init().init(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void init(String pathToConfigProp) throws IOException {
        input = new FileInputStream(pathToConfigProp); //path to config.properties
        configProp.load(input);
        String mode = configProp.getProperty("mode").toLowerCase();
        String pathToTextFile = configProp.getProperty("pathToFile");
        String pathToMDBConf = configProp.getProperty("pathToMongoDbConfig");


        if ((mode.equals("1") || mode.equalsIgnoreCase("tokenizer")) || mode.equals("2") || mode.equalsIgnoreCase("indexer")) {
            if (pathToTextFile == null) {
                System.out.println("Please, check properties file. Key pathToFile is not exists or its value is empty");
                return;
            } else {
                if (!new File(pathToTextFile).exists()) {
                    System.out.println("File for with path is not found. Check it in config file");
                    return;
                }
            }
        }

        if ((mode.equals("2") || mode.equalsIgnoreCase("indexer")) ||
                mode.equals("3") || mode.equalsIgnoreCase("WordSearcher") ||
                mode.equals("4") || mode.equalsIgnoreCase("Searcher")) {

            if (pathToMDBConf == null) {
                System.out.println("Please, check properties file. Key pathToMongoDbConfig is not exists or its value is empty");
                return;
            } else {
                boolean isMongoDbConfigFileExists = new File(pathToMDBConf).exists();
                boolean isTextFileExists = new File(pathToTextFile).exists();

                if (!isMongoDbConfigFileExists || !isTextFileExists) {
                    System.out.println("Check paths to files in config file");
                    return;
                }
            }
        }

        List<String> lines;

        //первое задание
        if (mode.equals("1") || mode.equalsIgnoreCase("tokenizer")) {
            lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            WordSearcher s = new WordSearcher();
            s.printWordsFromText(pathToTextFile, lines);
        }

        //второе задание
        if (mode.equals("2") || mode.equalsIgnoreCase("indexer")) {
            mdb.initConnection(pathToMDBConf);

            /*if (!mdb.isAuthenticate()) {
                System.out.println("Connection refused. Check mongodb config file");
                return;
            }*/

            lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            WordSearcher s = new WordSearcher();
            List<Word> words = s.getWords(pathToTextFile, lines);
            Map<String, List<Positions>> content = s.getAllPositionOfWord(words);
            mdb.addIndex(content);
            System.out.println("All words in database");
        }

        //третье задание
        if (mode.equals("3") || mode.equalsIgnoreCase("WordSearcher")) {

            mdb.initConnection(pathToMDBConf);
            /*if (!mdb.isAuthenticate()) {
                System.out.println("Connection refused. Check mongodb config file");
                return;
            }*/

            Searcher searcher = new Searcher();
            System.out.println("Enter your search query:");
            String query = new Scanner(System.in).nextLine();
            Map<String, Map<String, List<Positions>>> findedDocuments = searcher.getPositionInDocument(mdb, query);
            if (findedDocuments.isEmpty()) {
                System.out.println("Nothing found on your request");
                return;
            }
            for (Map.Entry position : findedDocuments.entrySet()) {
                Map<String, List<Positions>> allPositions = (Map<String, List<Positions>>) position.getValue();
                boolean firstStep = true;
                for (Map.Entry pos : allPositions.entrySet()) {
                    for (Positions p : (List<Positions>) pos.getValue())
                        if (firstStep) {
                            System.out.println(p.getDocument() + "\n" + p.getLine() + ", " + p.getStart() + ", " + p.getEnd());
                            firstStep = false;
                        } else {
                            System.out.println(p.getLine() + ", " + p.getStart() + ", " + p.getEnd());
                        }
                }
            }
        }

        //четвертое задание
        if (mode.equals("4") || mode.equalsIgnoreCase("Searcher")) {
            mdb.initConnection(pathToMDBConf);

            /*if (!mdb.isAuthenticate()) {
                System.out.println("Connection refused. Check mongodb config file");
                return;
            }*/

            Searcher searcher = new Searcher();
            System.out.println("Enter your search query:");
            String query = new Scanner(System.in).nextLine();
            System.out.println("Enter context size:");

            int sizeContext = 0;
            do {
                if (sizeContext < 0) {
                    System.out.println("Context size should be >=0");
                }
                String input = new Scanner(System.in).nextLine();
                String pattern = "-?\\d+";
                if (input != null && !input.isEmpty() && input.matches(pattern)) {
                    sizeContext = Integer.parseInt(input);
                } else {
                    sizeContext = -1;
                }
            } while (sizeContext < 0);

            searcher.findQuoteInText(mdb, query, sizeContext);
        }
    }
}
