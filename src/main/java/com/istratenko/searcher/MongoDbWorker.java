package com.istratenko.searcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.istratenko.searcher.entity.Positions;
import com.mongodb.*;
import com.mongodb.util.JSON;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by denis on 19.03.17.
 */

public class MongoDbWorker {
    private static volatile MongoDbWorker instance;

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

    public MongoDbWorker() {

    }

    public static MongoDbWorker getInstance() {
        MongoDbWorker localInstance = instance;
        if (localInstance == null) {
            synchronized (MongoDbWorker.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MongoDbWorker();
                }
            }
        }
        return localInstance;
    }

    public void initConnection(String pathToConfigFile){
        try {
            input = new FileInputStream(pathToConfigFile);
            prop.load(input);

            // Создаем подключение
            if (mongoClient==null) {
                mongoClient = new MongoClient(prop.getProperty("host"), Integer.valueOf(prop.getProperty("port")));
            }

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

    public void addIndex(Map<String, List<Positions>> words) {
        for (Map.Entry<String, List<Positions>> word : words.entrySet()) {
            ObjectMapper mapper = new ObjectMapper();
            DBObject dboJack[] = mapper.convertValue(word.getValue(), BasicDBObject[].class);

            BasicDBObject positionOfWord=new BasicDBObject("$each", dboJack);
            DBObject listItem = new BasicDBObject("$addToSet", new BasicDBObject(word.getKey(), positionOfWord)); //for each new array element make addToSet

            BasicDBObject findQuery = new BasicDBObject();
            findQuery.put(word.getKey(), new BasicDBObject("$exists", true)); //if element exists, then retirn its
            table.update(findQuery, listItem, true, false); //upsert=true (make merge), multi=false
        }
    }

    public Map<String, List<Positions>> getIndex(String word) {
        Map<String, List<Positions>> wordItems = new HashMap<>();
        BasicDBObject query = new BasicDBObject();
        query.put(word, new BasicDBObject("$exists", true));

        DBObject object = table.findOne(query);

        if (object != null) {
            String postitionValues = new JSON().serialize(object.get(word));
            List<Positions> positions;
            try {
                positions = new ObjectMapper().readValue(postitionValues, new TypeReference<ArrayList<Positions>>() {
                });
                wordItems.put(word, positions);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wordItems;
    }

    public boolean isAuthenticate() {
        return authenticate;
    }
}
