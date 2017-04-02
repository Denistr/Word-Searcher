Перед запуском:
1. Нужна версия java 1.8

Для запуска:
Для запуска программы нужно в консоли:
java -jar */target/WordSearcher-1.0-SNAPSHOT-jar-with-dependencies.jar путь до конфигурационного файла config.properties


Конфигурационный файл config.properties. Пример структуры - */src/main/resources/config.properties
В этом файле:
mode - режим работы программы.
1 или tokenizer  соответствует первой программе из задания
2 или indexer - соотвествует второй программе из задания
3 или wordsearcher - третье программе из задания
4 или searcher  - 4 программе из задания

pathToFile - путь к текстовому файлу
pathToMongoDbConfig - путь к конфигурационному файлу для работы  с Mongo (mongoConfog.properties)

Путь до файлов не должен содержать пробелов/русских букв
Пример: 
Работает - /home/denis/NewText.txt
Не работает - /home/denis/Рабочий Стол/NewText.txt



Конфигурационный файл mongoConfig.properties. Пример структуры - */src/main/resources/mongoConfig.properties
В этом файле:
host - ip адрес сервера
port - порт, на котором запущен сервер Монго
dbname - имя базы данных
login - логин
password - пароль
table - коллекция, с которой будет осуществляться работа
