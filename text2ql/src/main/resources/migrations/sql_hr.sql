--
-- PostgreSQL database dump
--

-- Dumped from database version 14.9 (Ubuntu 14.9-0ubuntu0.22.04.1)
-- Dumped by pg_dump version 14.9 (Ubuntu 14.9-0ubuntu0.22.04.1)

--
-- Name: cities; Type: TABLE; Schema: hr; Owner: -
--

create schema hr;
create
extension ltree schema hr;

CREATE TABLE hr.cities
(
    id        uuid                   NOT NULL,
    region_id uuid,
    code      character varying(16)  NOT NULL,
    name      character varying(256) NOT NULL
);


--
-- Name: departments; Type: TABLE; Schema: hr; Owner: -
--

CREATE TABLE hr.departments
(
    id          uuid                   NOT NULL,
    location_id uuid,
    code        character varying(16)  NOT NULL,
    name        character varying(512) NOT NULL,
    path        hr.ltree
);


--
-- Name: employees; Type: TABLE; Schema: hr; Owner: -
--

CREATE TABLE hr.employees
(
    id            uuid                   NOT NULL,
    job_id        uuid,
    department_id uuid,
    gender        boolean                NOT NULL,
    name          character varying(128) NOT NULL,
    email         character varying(128) NOT NULL,
    hired_date    timestamp without time zone NOT NULL,
    fired         boolean DEFAULT false,
    fired_date    timestamp without time zone,
    path          hr.ltree
);


--
-- Name: job_functions; Type: TABLE; Schema: hr; Owner: -
--

CREATE TABLE hr.job_functions
(
    id   uuid                   NOT NULL,
    code character varying(16)  NOT NULL,
    name character varying(256) NOT NULL
);


--
-- Name: jobs; Type: TABLE; Schema: hr; Owner: -
--

CREATE TABLE hr.jobs
(
    id          uuid                   NOT NULL,
    function_id uuid,
    code        character varying(16)  NOT NULL,
    name        character varying(256) NOT NULL
);


--
-- Name: locations; Type: TABLE; Schema: hr; Owner: -
--

CREATE TABLE hr.locations
(
    id      uuid                   NOT NULL,
    city_id uuid,
    code    character varying(16)  NOT NULL,
    name    character varying(256) NOT NULL
);


--
-- Name: regions; Type: TABLE; Schema: hr; Owner: -
--

CREATE TABLE hr.regions
(
    id   uuid                   NOT NULL,
    code character varying(16)  NOT NULL,
    name character varying(256) NOT NULL
);


--
-- Data for Name: cities; Type: TABLE DATA; Schema: hr; Owner: -
--

INSERT INTO hr.cities
VALUES ('a0639608-527a-11ee-be56-0242ac120002', '66e641dc-527a-11ee-be56-0242ac120002', '1', 'city1');
INSERT INTO hr.cities
VALUES ('a0639608-527a-11ee-be56-0242ac120003', '66e641dc-527a-11ee-be56-0242ac120002', '2', 'city2');
INSERT INTO hr.cities
VALUES ('66e641dc-527a-11ee-be56-0242ac120004', '66e641dc-527a-11ee-be56-0242ac120003', '3', 'city3');
INSERT INTO hr.cities
VALUES ('66e641dc-527a-11ee-be56-0242ac120005', '66e641dc-527a-11ee-be56-0242ac120003', '4', 'city4');


--
-- Data for Name: departments; Type: TABLE DATA; Schema: hr; Owner: -
--

INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120002', '0a61d47a-527b-11ee-be56-0242ac120002', '1', 'dep1', '1');
INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120003', '0a61d47a-527b-11ee-be56-0242ac120003', '2', 'dep2', '1.2');
INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120004', '0a61d47a-527b-11ee-be56-0242ac120004', '3', 'dep3', '1.3');
INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120005', '0a61d47a-527b-11ee-be56-0242ac120005', '4', 'dep4', '1.4');
INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120006', '0a61d47a-527b-11ee-be56-0242ac120006', '5', 'dep5', '1.5');
INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120007', '0a61d47a-527b-11ee-be56-0242ac120006', '6', 'dep6', '1.5.6');
INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120008', '0a61d47a-527b-11ee-be56-0242ac120006', '7', 'dep7', '1.5.7');
INSERT INTO hr.departments
VALUES ('aebb311a-527b-11ee-be56-0242ac120009', '0a61d47a-527b-11ee-be56-0242ac120006', '8', 'dep8', '1.5.8');


--
-- Data for Name: employees; Type: TABLE DATA; Schema: hr; Owner: -
--

INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120000', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120002', true, 'employee1', 'employee1@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120001', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120003', true, 'employee2', 'employee2@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee2');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120002', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120004', true, 'employee3', 'employee3@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee3');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120003', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120005', true, 'employee4', 'employee4@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee4');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120004', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120006', true, 'employee5', 'employee5@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee5');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120005', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120007', true, 'employee6', 'employee6@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee6');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120006', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120008', true, 'employee7', 'employee7@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee7');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120007', '5ddd0a88-527c-11ee-be56-0242ac120002',
        'aebb311a-527b-11ee-be56-0242ac120009', true, 'employee8', 'employee8@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee8');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120008', '5ddd0a88-527c-11ee-be56-0242ac120003',
        'aebb311a-527b-11ee-be56-0242ac120009', true, 'employee9', 'employee9@mail.com', '2023-09-14 02:34:23', false,
        NULL, 'employee1.employee8.employee9');
INSERT INTO hr.employees
VALUES ('8804aac8-527c-11ee-be56-0242ac120009', '5ddd0a88-527c-11ee-be56-0242ac120003',
        'aebb311a-527b-11ee-be56-0242ac120009', false, 'employee10', 'employee10@mail.com', '2023-09-14 02:34:23',
        false, NULL, 'employee1.employee8.employee10');


--
-- Data for Name: job_functions; Type: TABLE DATA; Schema: hr; Owner: -
--

INSERT INTO hr.job_functions
VALUES ('202a28c4-527c-11ee-be56-0242ac120002', '1', 'department_master');
INSERT INTO hr.job_functions
VALUES ('202a28c4-527c-11ee-be56-0242ac120003', '2', 'department_slave');


--
-- Data for Name: jobs; Type: TABLE DATA; Schema: hr; Owner: -
--

INSERT INTO hr.jobs
VALUES ('5ddd0a88-527c-11ee-be56-0242ac120002', '202a28c4-527c-11ee-be56-0242ac120002', '1', 'master');
INSERT INTO hr.jobs
VALUES ('5ddd0a88-527c-11ee-be56-0242ac120003', '202a28c4-527c-11ee-be56-0242ac120003', '2', 'slave');


--
-- Data for Name: locations; Type: TABLE DATA; Schema: hr; Owner: -
--

INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120002', 'a0639608-527a-11ee-be56-0242ac120002', '1', 'location1');
INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120003', 'a0639608-527a-11ee-be56-0242ac120002', '2', 'location2');
INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120004', 'a0639608-527a-11ee-be56-0242ac120003', '3', 'location3');
INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120005', '66e641dc-527a-11ee-be56-0242ac120004', '4', 'location4');
INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120006', '66e641dc-527a-11ee-be56-0242ac120004', '5', 'location5');
INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120007', '66e641dc-527a-11ee-be56-0242ac120004', '6', 'location6');
INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120008', '66e641dc-527a-11ee-be56-0242ac120005', '7', 'location7');
INSERT INTO hr.locations
VALUES ('0a61d47a-527b-11ee-be56-0242ac120009', '66e641dc-527a-11ee-be56-0242ac120005', '8', 'location8');


--
-- Data for Name: regions; Type: TABLE DATA; Schema: hr; Owner: -
--

INSERT INTO hr.regions
VALUES ('66e641dc-527a-11ee-be56-0242ac120002', '1', 'region1');
INSERT INTO hr.regions
VALUES ('66e641dc-527a-11ee-be56-0242ac120003', '2', 'region2');


--
-- Name: cities cities_pkey; Type: CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.cities
    ADD CONSTRAINT cities_pkey PRIMARY KEY (id);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: employees employee_pkey; Type: CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.employees
    ADD CONSTRAINT employee_pkey PRIMARY KEY (id);


--
-- Name: job_functions job_functions_pkey; Type: CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.job_functions
    ADD CONSTRAINT job_functions_pkey PRIMARY KEY (id);


--
-- Name: jobs jobs_pkey; Type: CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.jobs
    ADD CONSTRAINT jobs_pkey PRIMARY KEY (id);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);


--
-- Name: regions regions_pkey; Type: CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.regions
    ADD CONSTRAINT regions_pkey PRIMARY KEY (id);


--
-- Name: cities cities_region_id_fkey; Type: FK CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.cities
    ADD CONSTRAINT cities_region_id_fkey FOREIGN KEY (region_id) REFERENCES hr.regions(id);


--
-- Name: departments departments_location_id_fkey; Type: FK CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.departments
    ADD CONSTRAINT departments_location_id_fkey FOREIGN KEY (location_id) REFERENCES hr.locations(id);


--
-- Name: employees employee_department_id_fkey; Type: FK CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.employees
    ADD CONSTRAINT employee_department_id_fkey FOREIGN KEY (department_id) REFERENCES hr.departments(id);


--
-- Name: employees employee_job_id_fkey; Type: FK CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.employees
    ADD CONSTRAINT employee_job_id_fkey FOREIGN KEY (job_id) REFERENCES hr.jobs(id);


--
-- Name: jobs jobs_function_id_fkey; Type: FK CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.jobs
    ADD CONSTRAINT jobs_function_id_fkey FOREIGN KEY (function_id) REFERENCES hr.job_functions(id);


--
-- Name: locations locations_city_id_fkey; Type: FK CONSTRAINT; Schema: hr; Owner: -
--

ALTER TABLE ONLY hr.locations
    ADD CONSTRAINT locations_city_id_fkey FOREIGN KEY (city_id) REFERENCES hr.cities(id);


--
-- PostgreSQL database dump complete
--

