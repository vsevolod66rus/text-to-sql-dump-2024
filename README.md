# Материалы к [докладу](https://dump-ekb.ru/analogov-net-sdelali-korporativnoe-text-to-sql-reshenie)

Небольшой кусок внутреннего dsl для конструктора SQL-запросов на основе выделенных сущностей из запроса на русском языке

### дамп данных [тут](https://github.com/vsevolod66rus/text-to-sql-dump-2024/blob/master/text2ql/src/main/resources/migrations/dump_hr.sql)
### внутренняя схема домена [тут](https://github.com/vsevolod66rus/text-to-sql-dump-2024/blob/master/text2ql/src/main/resources/domain_schema_hr.yml) 
### построение запроса [тут](https://github.com/vsevolod66rus/text-to-sql-dump-2024/blob/master/text2ql/src/main/scala/text2ql/dao/postgres/QueryBuilder.scala#L152)

можно запустить, указав креды postgresql [тут](https://github.com/vsevolod66rus/text-to-sql-dump-2024/blob/master/text2ql/src/main/resources/params.conf), по-умолчанию сваггер будет доступен на http://127.0.0.1:3000/docs/ 

Пример запроса на построение sql:
```
curl -X 'POST' \
'http://127.0.0.1:3000/api/v1/domainSchema/relationsCheck/HR?onlySet=true' \
-H 'accept: application/json' \
-H 'Content-Type: application/octet-stream' \
--data-binary '@domain_schema_hr.yml'
```
Ответ:
```
[
  {
    "tables": [
      "employees",
      "countries",
      "departments",
      "jobs",
      "job_functions",
      "cities"
    ],
    "queries": {
      "generalQuery": "select * from (select row_number() over() as cursor, * from (select employees.gender as employees_gender, employees.salary as employees_salary, employees.id as employees_id, employees.hired_date as employees_hired_date, employees.citizenship as employees_citizenship, employees.fired_date as employees_fired_date, employees.full_name as employees_full_name, employees.email as employees_email, employees.parent_id as employees_parent_id, employees.age as employees_age, employees.height as employees_height, employees.department_id as employees_department_id, employees.job_id as employees_job_id, employees.fired as employees_fired, countries.id as countries_id, countries.name as countries_name, departments.id as departments_id, departments.city_id as departments_city_id, departments.name as departments_name, departments.parent_id as departments_parent_id, jobs.id as jobs_id, jobs.function_id as jobs_function_id, jobs.name as jobs_name, job_functions.id as job_functions_id, job_functions.name as job_functions_name, cities.id as cities_id, cities.country_id as cities_country_id, cities.name as cities_name from ( select * from hr.employees ) as employees  inner join ( select * from hr.departments ) as departments on departments.id = employees.department_id inner join ( select * from hr.jobs ) as jobs on jobs.id = employees.job_id inner join ( select * from hr.cities ) as cities on cities.id = departments.city_id inner join ( select * from hr.job_functions ) as job_functions on job_functions.id = jobs.function_id inner join ( select * from hr.countries ) as countries on countries.id = cities.country_id  order by employees.id asc) as sorted_res) as res where res.cursor > 0 and res.cursor < 11;",
      "countQuery": "select count(*), count(distinct EMPLOYEES.id) from ( select * from hr.employees ) as employees  inner join ( select * from hr.departments ) as departments on departments.id = employees.department_id inner join ( select * from hr.jobs ) as jobs on jobs.id = employees.job_id inner join ( select * from hr.cities ) as cities on cities.id = departments.city_id inner join ( select * from hr.job_functions ) as job_functions on job_functions.id = jobs.function_id inner join ( select * from hr.countries ) as countries on countries.id = cities.country_id "
    },
    "result": {
      "properties": [
        {
          "key": "employees_gender",
          "title": "Пол сотрудника",
          "dataType": "string"
        },
        {
          "key": "employees_salary",
          "title": "Зарплата сотрудника (руб/мес)",
          "dataType": "double"
        },
        {
          "key": "employees_id",
          "title": "Внутренний id сотрудника",
          "dataType": "string"
        },
        {
          "key": "employees_hired_date",
          "title": "Дата устройства на работу",
          "dataType": "datetime"
        },
        {
          "key": "employees_citizenship",
          "title": "Гражданство сотрудника",
          "dataType": "string"
        },
        {
          "key": "employees_fired_date",
          "title": "Дата увольнения сотрудника",
          "dataType": "datetime"
        },
        {
          "key": "employees_full_name",
          "title": "Полное имя сотрудника",
          "dataType": "string"
        },
        {
          "key": "employees_email",
          "title": "Почта сотрудника",
          "dataType": "string"
        },
        {
          "key": "employees_parent_id",
          "title": "Ссылка на начальника",
          "dataType": "string"
        },
        {
          "key": "employees_age",
          "title": "Возраст сотрудника",
          "dataType": "long"
        },
        {
          "key": "employees_height",
          "title": "Рост сотрудника",
          "dataType": "long"
        },
        {
          "key": "employees_department_id",
          "title": "Ссылка на департамент",
          "dataType": "string"
        },
        {
          "key": "employees_job_id",
          "title": "Ссылка на должность",
          "dataType": "string"
        },
        {
          "key": "employees_fired",
          "title": "Уволен?",
          "dataType": "boolean"
        },
        {
          "key": "countries_id",
          "title": "Внутренний id страны",
          "dataType": "string"
        },
        {
          "key": "countries_name",
          "title": "Страна",
          "dataType": "string"
        },
        {
          "key": "departments_id",
          "title": "Внутренний id департамента",
          "dataType": "string"
        },
        {
          "key": "departments_city_id",
          "title": "Ссылка на id города",
          "dataType": "string"
        },
        {
          "key": "departments_name",
          "title": "Департамент",
          "dataType": "string"
        },
        {
          "key": "departments_parent_id",
          "title": "Ссылка на руководящий департамент",
          "dataType": "string"
        },
        {
          "key": "jobs_id",
          "title": "Внутренний id должности",
          "dataType": "string"
        },
        {
          "key": "jobs_function_id",
          "title": "Ссылка на рабочую функцию",
          "dataType": "string"
        },
        {
          "key": "jobs_name",
          "title": "Должность",
          "dataType": "string"
        },
        {
          "key": "job_functions_id",
          "title": "Внутренний id функции",
          "dataType": "string"
        },
        {
          "key": "job_functions_name",
          "title": "Рабочая функция",
          "dataType": "string"
        },
        {
          "key": "cities_id",
          "title": "Внутренний id города",
          "dataType": "string"
        },
        {
          "key": "cities_country_id",
          "title": "Ссылка на внутренний id страны",
          "dataType": "string"
        },
        {
          "key": "cities_name",
          "title": "Город",
          "dataType": "string"
        }
      ],
      "items": [
        {
          "jobs_name": "Водитель",
          "employees_id": "0003bef7-05b1-402c-b201-dc21b11dfc34",
          "employees_hired_date": "1991-12-26T09:00:14Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент логистики",
          "departments_id": "2e707ab4-4e7e-447c-9f7e-d4f919f2da67",
          "employees_email": "СвятославСвятополковичСолнцев-959151945@yandex.ru",
          "employees_age": 38,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Санкт-Петербург",
          "employees_department_id": "2e707ab4-4e7e-447c-9f7e-d4f919f2da67",
          "employees_job_id": "43854dd6-05dc-4d67-b014-cd2be5453705",
          "cities_id": "ba7adf4c-2dbe-444e-a14d-8bcafd895ce1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "43854dd6-05dc-4d67-b014-cd2be5453705",
          "job_functions_name": "Грузоперевозки",
          "employees_salary": 243269.81,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "7b3faba5-3529-47e7-8f13-3511875609e8",
          "employees_full_name": "Святослав Святополкович Солнцев",
          "employees_parent_id": "a03bd3ca-f4c0-40ab-afdf-ad67a708d4eb",
          "id": "0246f719-2c46-4ed4-9855-88e606bd35bb",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "7b3faba5-3529-47e7-8f13-3511875609e8",
          "employees_height": 188,
          "departments_city_id": "ba7adf4c-2dbe-444e-a14d-8bcafd895ce1"
        },
        {
          "jobs_name": "Штурмовик",
          "employees_id": "00040d93-e553-4358-9c73-6f0ffb2f9a82",
          "employees_hired_date": "1991-12-26T09:00:20Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "ГрадиславРадомысловичКабанов954035022@yandex.ru",
          "employees_age": 20,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 285822.25,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Градислав Радомыслович Кабанов",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "1b5f960c-f63d-4c9f-964d-f4f872f6162f",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 161,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        },
        {
          "jobs_name": "Штурмовик",
          "employees_id": "000787c9-b6f8-4bdd-8c62-63fff4de180e",
          "employees_hired_date": "1991-12-26T09:00:20Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "ДобрынСвятославовичДаркхолм1086346719@yandex.ru",
          "employees_age": 31,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 306080.7,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Добрын Святославович Даркхолм",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "60e6a8eb-9991-4d16-8b69-e74778a15cfe",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 178,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        },
        {
          "jobs_name": "Механик",
          "employees_id": "0010ff78-588e-480b-81e4-4ff069d8f306",
          "employees_hired_date": "1991-12-26T09:00:25Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "ДемидИстиславовичМакэвой494662475@yandex.ru",
          "employees_age": 29,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "c39b9d5f-7b9c-49cf-9127-93446127f30a",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "c39b9d5f-7b9c-49cf-9127-93446127f30a",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 269383.25,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Демид Истиславович Макэвой",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "731bfdd0-018d-46f7-a1e6-50e718321bc9",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 174,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        },
        {
          "jobs_name": "Штурмовик",
          "employees_id": "00116004-08fb-40fa-931c-5d4ba3ca3865",
          "employees_hired_date": "1991-12-26T09:00:20Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "ГомоборТихомировичМилос1191475554@yandex.ru",
          "employees_age": 25,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 221204.17,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Гомобор Тихомирович Милос",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "17e15e8a-91ef-40e3-8e97-2387be4c73bc",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 178,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        },
        {
          "jobs_name": "Штурмовик",
          "employees_id": "0012e494-0d1b-4839-921a-e151a22ad1af",
          "employees_hired_date": "1991-12-26T09:00:20Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "ЯромирВячеславовичМудрый-673753927@yandex.ru",
          "employees_age": 30,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 201391.14,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Яромир Вячеславович Мудрый",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "1d207e78-d0b5-4a41-b86b-da34eb10ae80",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 162,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        },
        {
          "jobs_name": "Медбрат",
          "employees_id": "0015af05-6d8b-42a9-9bba-997c45e255c4",
          "employees_hired_date": "1991-12-26T09:00:12Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент здравоохранения",
          "departments_id": "a4583bb1-74ea-4f8b-8e7a-71c8648a43ac",
          "employees_email": "ЯромирОсмомысловичНазаров181813029@yandex.ru",
          "employees_age": 39,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Екатеринбург",
          "employees_department_id": "a4583bb1-74ea-4f8b-8e7a-71c8648a43ac",
          "employees_job_id": "27e658bc-b991-4f12-96dc-fe8313fc7b36",
          "cities_id": "e5e4b5a0-f857-4840-887b-95f30d642c44",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "27e658bc-b991-4f12-96dc-fe8313fc7b36",
          "job_functions_name": "Терапия",
          "employees_salary": 123178.805,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "b866920e-b6a7-4b9a-99bb-cd7dbeeb5a2d",
          "employees_full_name": "Яромир Осмомыслович Назаров",
          "employees_parent_id": "813bd67e-c023-45c7-bf37-9f447337d6df",
          "id": "7b63d26f-4f50-4296-ba07-607f7aff12b2",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "b866920e-b6a7-4b9a-99bb-cd7dbeeb5a2d",
          "employees_height": 179,
          "departments_city_id": "e5e4b5a0-f857-4840-887b-95f30d642c44"
        },
        {
          "jobs_name": "Штурмовик",
          "employees_id": "0018f853-45ed-4afe-aa0a-fcca9704b96f",
          "employees_hired_date": "1991-12-26T09:00:20Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "ДушанДушановичСветов-1048500090@yandex.ru",
          "employees_age": 35,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 275754.03,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Душан Душанович Светов",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "6ed87a3c-4dc9-4461-b59a-db016783416d",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 166,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        },
        {
          "jobs_name": "Штурмовик",
          "employees_id": "001c1a04-10a7-4115-92fd-1b47b7ae7a0e",
          "employees_hired_date": "1991-12-26T09:00:20Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "ГотикаславМедведославовичГодунов1658802434@yandex.ru",
          "employees_age": 21,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 237510.17,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Готикаслав Медведославович Годунов",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "5b83faa6-63e7-4e01-8c24-106a79acb850",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 165,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        },
        {
          "jobs_name": "Штурмовик",
          "employees_id": "001e97e8-d2a6-42ff-96de-eef6bed04017",
          "employees_hired_date": "1991-12-26T09:00:20Z",
          "employees_fired_date": "нет данных",
          "departments_name": "Департамент обороны и нападения",
          "departments_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_email": "СвятополкВерославовичГослинг1719503565@yandex.ru",
          "employees_age": 38,
          "departments_parent_id": "83aa97ed-5cb1-4589-abb2-5470297faf93",
          "countries_name": "Русь",
          "cities_name": "Москва",
          "employees_department_id": "c3e5ebdc-4351-46b7-bc55-d815def6da3a",
          "employees_job_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "cities_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1",
          "employees_fired": false,
          "employees_gender": "M",
          "jobs_id": "ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158",
          "job_functions_name": "Оборона и нападение",
          "employees_salary": 312169.03,
          "employees_citizenship": "Русь",
          "cities_country_id": "151a31da-c350-4296-9e63-21db586dee83",
          "jobs_function_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_full_name": "Святополк Верославович Гослинг",
          "employees_parent_id": "aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e",
          "id": "a43fb615-7298-49ce-875f-831b8ee1b46a",
          "countries_id": "151a31da-c350-4296-9e63-21db586dee83",
          "job_functions_id": "512075d8-0c93-47b6-b7a6-0d00e8fb4dca",
          "employees_height": 173,
          "departments_city_id": "6ce2ea71-a255-483f-b569-3a15354ab2b1"
        }
      ],
      "total": 25021
    }
  }
]
```