package com.istratenko.searcher.indexer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.reflect.TypeToken;
import com.istratenko.searcher.tokenizer.Positions;
import com.istratenko.searcher.tokenizer.WordItem;
import com.mongodb.*;
import com.mongodb.util.JSON;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by denis on 19.03.17.
 */

public class MongoDbWorker {
    // это клиент который обеспечит подключение к БД
    private MongoClient mongoClient;

    // В нашем случае, этот класс дает
    // возможность аутентифицироваться в MongoDB
    private DB db;

    // тут мы будем хранить состояние подключения к БД
    private boolean authenticate;

    // И класс который обеспечит возможность работать
    // с коллекциями / таблицами MongoDB
    private DBCollection table;

    Properties prop = new Properties();
    InputStream input = null;

    public MongoDbWorker(String pathToConfigFile) {
        try {
            input = new FileInputStream(pathToConfigFile);
            prop.load(input);

            // Создаем подключение
            mongoClient = new MongoClient(prop.getProperty("host"), Integer.valueOf(prop.getProperty("port")));

            // Выбираем БД для дальнейшей работы
            db = mongoClient.getDB(prop.getProperty("dbname"));

            // Входим под созданным логином и паролем
            authenticate = db.authenticate(prop.getProperty("login"), prop.getProperty("password").toCharArray());

            // Выбираем коллекцию/таблицу для дальнейшей работы
            table = db.getCollection(prop.getProperty("table"));

        } catch (UnknownHostException e) {
            System.err.println("Don't connect!");
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


    //TODO: мапа не хранит повторяющиеся значения. составлять ArrayList Positions
    public void addIndex(Map<String, List<Positions>> words) {
        for (Map.Entry<String, List<Positions>> word : words.entrySet()){
            BasicDBObject document = new BasicDBObject();
            ObjectMapper mapper = new ObjectMapper();
            DBObject dboJack[] = mapper.convertValue(word.getValue(), BasicDBObject[].class);
            document.put(word.getKey(), dboJack);
            table.insert(document);
        }
    }

    public Map<String, List<Positions>>  getIndex(String word) {
        BasicDBObject query = new BasicDBObject();
        Map<String, List<Positions>> wordItems = new HashMap<>();

        query.put(word, new BasicDBObject("$exists", true));

        DBCursor dbCursor = table.find(query);
        if (dbCursor != null) {
            while (dbCursor.hasNext()) {
                String postitionValues = new JSON().serialize(dbCursor.next().get(word));

                try {
                    List<Positions> positions = new ObjectMapper().readValue(postitionValues, new TypeReference<ArrayList<Positions>>() { });
                    wordItems.put(word,positions);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return wordItems;
    }

    public boolean isAuthenticate() {
        return authenticate;
    }
}
