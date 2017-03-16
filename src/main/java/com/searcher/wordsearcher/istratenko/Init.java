package com.searcher.wordsearcher.istratenko;

/**
 * Created by denis on 16.03.17.
 */
public class Init {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Set only one parameter on input");
            return;
        }

        WordSearcher s = new WordSearcher();

        try {
            s.printWordsFromText(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
