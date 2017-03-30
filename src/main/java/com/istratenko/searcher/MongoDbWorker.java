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

    private DB db;

    // состояние подключения к БД
    private boolean authenticate;

    //класс, позволяющий работать с коллекциями
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

    public void initConnection(String pathToConfigFile) {
        try {
            input = new FileInputStream(pathToConfigFile);
            prop.load(input);

            // Создаем подключение
            if (mongoClient == null) {
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

    /**
     * добавляет Map: key - слово, value - массив позиций  в базу данных
     * @param words Map: key - слово, value - массив позиций
     */
    public void addIndex(Map<String, List<Positions>> words) {
        for (Map.Entry<String, List<Positions>> word : words.entrySet()) {
            ObjectMapper mapper = new ObjectMapper();
            DBObject dboJack[] = mapper.convertValue(word.getValue(), BasicDBObject[].class);

            BasicDBObject positionOfWord = new BasicDBObject("$each", dboJack);
            DBObject listItem = new BasicDBObject("$addToSet", new BasicDBObject(word.getKey(), positionOfWord)); //for each new array element make addToSet

            BasicDBObject findQuery = new BasicDBObject();
            findQuery.put(word.getKey(), new BasicDBObject("$exists", true)); //if element exists, then return its
            table.update(findQuery, listItem, true, false); //upsert=true (make merge), multi=false
        }
    }

    /**
     * позволяет получить конкретную Map: key - слово, value - массив позиций из базу данных (по заданному слову)
     * @param word слово, по котором происходит поиск в базе среди ключей
     * @return Map: key - слово, value - массив позиций этого слова
     */
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

    /**
     * позволяет получить все записи из базы данных
     * @return Map: key - слово, value - массив позиций этого слова
     * @throws IOException
     */
    public Map<String, List<Positions>> getAllWords() throws IOException {
        Map<String, List<Positions>> allWords = new HashMap<>();
        Map<String, List<Positions>> finalMap = new HashMap<>();
        DBCursor cursor = table.find();
        while (cursor.hasNext()) {
            allWords.putAll(cursor.next().toMap());

        }

        if (allWords.containsKey("_id")) {
            allWords.remove("_id");
        }

        for (Map.Entry m : allWords.entrySet()) {
            String postitionValues = new JSON().serialize(m.getValue());
            List<Positions> positions = new ObjectMapper().readValue(postitionValues, new TypeReference<ArrayList<Positions>>() {
            });
            finalMap.put((String)m.getKey(), positions);
        }
        return finalMap;
    }

    public boolean isAuthenticate() {
        return authenticate;
    }
}
