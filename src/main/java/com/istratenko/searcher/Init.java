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


    private void init(String pathToConfigProp) throws IOException {
        input = new FileInputStream(pathToConfigProp); //path to config.properties
        configProp.load(input);
        String mode = configProp.getProperty("mode").toLowerCase();
        List<String> lines;
        if (mode.equals("1") || mode.equalsIgnoreCase("tokenizer")) {
            String pathToTextFile = configProp.getProperty("pathToFile");
            boolean isExistsFile = new File(pathToTextFile).exists();
            if (!isExistsFile) {
                System.out.println("File for with path is not found. Che it in config file");
                return;
            }
            lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            WordSearcher s = new WordSearcher();
            s.printWordsFromText(pathToTextFile, lines);
        }

        if (mode.equals("2") || mode.equalsIgnoreCase("indexer")) {
            String pathToTextFile = configProp.getProperty("pathToFile");
            String pathToMDBConf = configProp.getProperty("pathToMongoDbConfig");

            boolean isMongoDbConfigFileExists = new File(pathToMDBConf).exists();
            boolean isTextFileExists = new File(pathToTextFile).exists();

            if (!isMongoDbConfigFileExists || !isTextFileExists) {
                System.out.println("Check paths to files in config file");
                return;
            }

            mdb.initConnection(pathToMDBConf);

            if (!mdb.isAuthenticate()) {
                System.out.println("Connection refused. Check mongodb config file");
                return;
            }


            lines = Files.readAllLines(Paths.get(pathToTextFile), StandardCharsets.UTF_8); //get list of lines, which contains in Text Document
            WordSearcher s = new WordSearcher();
            List<Word> words = s.getWords(pathToTextFile, lines);
            Map<String, List<Positions>> content = s.getAllPositionOfWord(words);
            mdb.addIndex(content);
        }

        if (mode.equals("3") || mode.equalsIgnoreCase("WordSearcher")) {
            String pathToMDBConf = configProp.getProperty("pathToMongoDbConfig");

            boolean isMongoDbConfigFileExists = new File(pathToMDBConf).exists();

            if (!isMongoDbConfigFileExists) {
                System.out.println("Config file for mongodb is not found. Check it in config file");
                return;
            }

            mdb.initConnection(pathToMDBConf);
            if (!mdb.isAuthenticate()) {
                System.out.println("Connection refused. Check mongodb config file");
                return;
            }

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

        if (mode.equals("4") || mode.equalsIgnoreCase("Searcher")) {
            String pathToMDBConf = configProp.getProperty("pathToMongoDbConfig");
            mdb.initConnection(pathToMDBConf);
            if (!mdb.isAuthenticate()) {
                System.out.println("Connection refused. Check mongodb config file");
                return;
            }

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
                if (input != null && !input.isEmpty()) {
                    sizeContext = Integer.parseInt(input);
                } else {
                    sizeContext = -1;
                }
            } while (sizeContext < 0);

            searcher.findQuoteInText(mdb, query, sizeContext);
        }
    }
}
