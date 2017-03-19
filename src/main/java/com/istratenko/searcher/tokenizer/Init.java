package com.istratenko.searcher.tokenizer;

import com.istratenko.searcher.indexer.MongoDbWorker;
import javafx.geometry.Pos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by denis on 16.03.17.
 */
public class Init {
    public static void main(String[] args) {

        MongoDbWorker mdb=new MongoDbWorker(args[1]);
        System.out.println(mdb.isAuthenticate());

        /*if (args.length != 1) {
            System.out.println("Set one parameter on input");
            return;
        }*/

        WordSearcher s = new WordSearcher();

        try {
            //s.printWordsFromText(args[0]);
            List<WordItem> words = s.getWordsFromFile(args[0]);
            Map<String, List<Positions>> content = s.getAllPositionOfWord(words);
            mdb.addIndex(content);
            mdb.getIndex(words.get(0).getWord());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
