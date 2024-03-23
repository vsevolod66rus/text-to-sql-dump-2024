# R&D: Handling dynamic sql in Scala within text2ql problem

## proof-of-concept для [доклада](https://github.com/vsevolod66rus/text2ql-examples/blob/main/%D0%9F%D1%80%D0%B5%D0%B7%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D0%B8%D1%8F_v1.0.0-RC1.pdf)

## работа с динамическими sql [тут](https://github.com/vsevolod66rus/text2ql-examples/blob/main/text2ql/src/main/scala/text2ql/dao/postgres/QueryManager.scala)

### для запуска укажите креды postgresql [тут](https://github.com/vsevolod66rus/text2ql-examples/blob/main/text2ql/src/main/resources/params.conf), при старте произойдет [миграция](https://github.com/vsevolod66rus/text2ql-examples/tree/main/text2ql/src/main/resources/migrations), сваггер будет доступен на http://127.0.0.1:3000/docs/ 
### схема домена [тут](https://github.com/vsevolod66rus/text2ql-examples/blob/main/text2ql/src/main/resources/domain_schema_hr.yml)
### typeDB-server для старта не нужен - клиент ленивый