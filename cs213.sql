--
-- PostgreSQL database dump
--

-- Dumped from database version 14.4
-- Dumped by pg_dump version 14.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ad_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ad_classes (
    ad_class_id smallint NOT NULL,
    ad_class_chinese_name character varying(40),
    ad_class_english_name character varying(40)
);


ALTER TABLE public.ad_classes OWNER TO postgres;

--
-- Name: class_teachers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.class_teachers (
    class_timetable_id integer,
    teacher_id integer
);


ALTER TABLE public.class_teachers OWNER TO postgres;

--
-- Name: class_timetable; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.class_timetable (
    class_timetable_id integer NOT NULL,
    class_id integer,
    location_id smallint,
    time_begin numeric(2,0) NOT NULL,
    time_end numeric(2,0) NOT NULL,
    weekday numeric(1,0) NOT NULL
);


ALTER TABLE public.class_timetable OWNER TO postgres;

--
-- Name: class_timetable_class_timetable_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.class_timetable_class_timetable_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.class_timetable_class_timetable_id_seq OWNER TO postgres;

--
-- Name: class_timetable_class_timetable_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.class_timetable_class_timetable_id_seq OWNED BY public.class_timetable.class_timetable_id;


--
-- Name: class_week_list; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.class_week_list (
    class_timetable_id integer,
    week numeric(2,0) NOT NULL
);


ALTER TABLE public.class_week_list OWNER TO postgres;

--
-- Name: classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.classes (
    class_id integer NOT NULL,
    class_name character varying(80) NOT NULL,
    course_id character varying(10),
    capacity smallint,
    semester_id integer DEFAULT 0,
    left_capacity integer,
    CONSTRAINT classes_capacity_check CHECK ((capacity > 0))
);


ALTER TABLE public.classes OWNER TO postgres;

--
-- Name: classes_class_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.classes_class_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.classes_class_id_seq OWNER TO postgres;

--
-- Name: classes_class_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.classes_class_id_seq OWNED BY public.classes.class_id;


--
-- Name: course_select; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.course_select (
    sid integer,
    class_id integer,
    grade integer
);


ALTER TABLE public.course_select OWNER TO postgres;

--
-- Name: course_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.course_type (
    course_id character varying(10),
    type integer,
    major_id integer
);


ALTER TABLE public.course_type OWNER TO postgres;

--
-- Name: courses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.courses (
    hour smallint,
    credit numeric(1,0),
    course_name character varying(30) NOT NULL,
    dept_id integer,
    prerequisite character varying,
    course_id character varying(10) NOT NULL,
    grading character(2) DEFAULT 'HM'::bpchar,
    CONSTRAINT courses_credit_check CHECK (((credit >= (0)::numeric) AND (credit <= (8)::numeric))),
    CONSTRAINT courses_hour_check CHECK ((hour > 0))
);


ALTER TABLE public.courses OWNER TO postgres;

--
-- Name: departments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.departments (
    dept_id integer NOT NULL,
    department character varying(15)
);


ALTER TABLE public.departments OWNER TO postgres;

--
-- Name: departments_dept_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.departments_dept_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.departments_dept_id_seq OWNER TO postgres;

--
-- Name: departments_dept_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.departments_dept_id_seq OWNED BY public.departments.dept_id;


--
-- Name: locations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.locations (
    location_id integer NOT NULL,
    location character varying(40) NOT NULL
);


ALTER TABLE public.locations OWNER TO postgres;

--
-- Name: locations_location_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.locations_location_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.locations_location_id_seq OWNER TO postgres;

--
-- Name: locations_location_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.locations_location_id_seq OWNED BY public.locations.location_id;


--
-- Name: majors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.majors (
    major_id integer NOT NULL,
    major character varying(20),
    dept_id integer
);


ALTER TABLE public.majors OWNER TO postgres;

--
-- Name: majors_major_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.majors_major_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.majors_major_id_seq OWNER TO postgres;

--
-- Name: majors_major_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.majors_major_id_seq OWNED BY public.majors.major_id;


--
-- Name: semesters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.semesters (
    semester_id integer NOT NULL,
    semester_name character varying(20) NOT NULL,
    semester_begin date,
    semester_end date
);


ALTER TABLE public.semesters OWNER TO postgres;

--
-- Name: semesters_semester_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.semesters_semester_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.semesters_semester_id_seq OWNER TO postgres;

--
-- Name: semesters_semester_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.semesters_semester_id_seq OWNED BY public.semesters.semester_id;


--
-- Name: students; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.students (
    sid integer NOT NULL,
    ad_class_id smallint,
    gender character(1),
    first_name character varying(20),
    last_name character varying(20),
    major_id integer DEFAULT 34 NOT NULL,
    enrolled_date date DEFAULT CURRENT_DATE,
    user_id integer
);


ALTER TABLE public.students OWNER TO postgres;

--
-- Name: teachers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.teachers (
    user_id integer NOT NULL,
    first_name character varying(40),
    last_name character varying(40)
);


ALTER TABLE public.teachers OWNER TO postgres;

--
-- Name: teachers_teacher_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.teachers_teacher_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.teachers_teacher_id_seq OWNER TO postgres;

--
-- Name: teachers_teacher_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.teachers_teacher_id_seq OWNED BY public.teachers.user_id;


--
-- Name: class_timetable class_timetable_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_timetable ALTER COLUMN class_timetable_id SET DEFAULT nextval('public.class_timetable_class_timetable_id_seq'::regclass);


--
-- Name: classes class_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classes ALTER COLUMN class_id SET DEFAULT nextval('public.classes_class_id_seq'::regclass);


--
-- Name: departments dept_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments ALTER COLUMN dept_id SET DEFAULT nextval('public.departments_dept_id_seq'::regclass);


--
-- Name: locations location_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locations ALTER COLUMN location_id SET DEFAULT nextval('public.locations_location_id_seq'::regclass);


--
-- Name: majors major_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.majors ALTER COLUMN major_id SET DEFAULT nextval('public.majors_major_id_seq'::regclass);


--
-- Name: semesters semester_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.semesters ALTER COLUMN semester_id SET DEFAULT nextval('public.semesters_semester_id_seq'::regclass);


--
-- Name: teachers user_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teachers ALTER COLUMN user_id SET DEFAULT nextval('public.teachers_teacher_id_seq'::regclass);


--
-- Name: ad_classes ad_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ad_classes
    ADD CONSTRAINT ad_classes_pkey PRIMARY KEY (ad_class_id);


--
-- Name: class_timetable class_timetable_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_timetable
    ADD CONSTRAINT class_timetable_pkey PRIMARY KEY (class_timetable_id);


--
-- Name: classes classes_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_pk UNIQUE (course_id, class_name, semester_id);


--
-- Name: classes classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_pkey PRIMARY KEY (class_id);


--
-- Name: courses courses_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pk PRIMARY KEY (course_id);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (dept_id);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (location_id);


--
-- Name: majors majors_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.majors
    ADD CONSTRAINT majors_pk PRIMARY KEY (major_id);


--
-- Name: semesters semesters_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.semesters
    ADD CONSTRAINT semesters_pk PRIMARY KEY (semester_id);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (sid);


--
-- Name: teachers teachers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teachers
    ADD CONSTRAINT teachers_pkey PRIMARY KEY (user_id);


--
-- Name: class_timetable_class_timetable_id_class_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX class_timetable_class_timetable_id_class_id_index ON public.class_timetable USING btree (class_timetable_id, class_id);


--
-- Name: class_week_list_class_timetable_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX class_week_list_class_timetable_id_index ON public.class_week_list USING btree (class_timetable_id);


--
-- Name: course_select_sid_class_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX course_select_sid_class_id_index ON public.course_select USING btree (sid, class_id);


--
-- Name: course_select_sid_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX course_select_sid_index ON public.course_select USING btree (sid);


--
-- Name: majors_major_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX majors_major_uindex ON public.majors USING btree (major);


--
-- Name: class_teachers class_teachers_class_timetable_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_teachers
    ADD CONSTRAINT class_teachers_class_timetable_id_fkey FOREIGN KEY (class_timetable_id) REFERENCES public.class_timetable(class_timetable_id);


--
-- Name: class_teachers class_teachers_teacher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_teachers
    ADD CONSTRAINT class_teachers_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(user_id);


--
-- Name: class_timetable class_timetable_class_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_timetable
    ADD CONSTRAINT class_timetable_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.classes(class_id);


--
-- Name: class_timetable class_timetable_location_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_timetable
    ADD CONSTRAINT class_timetable_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(location_id);


--
-- Name: class_week_list class_week_list_class_timetable_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_week_list
    ADD CONSTRAINT class_week_list_class_timetable_id_fkey FOREIGN KEY (class_timetable_id) REFERENCES public.class_timetable(class_timetable_id);


--
-- Name: classes classes_courses_course_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_courses_course_id_fk FOREIGN KEY (course_id) REFERENCES public.courses(course_id);


--
-- Name: classes classes_semesters_semester_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_semesters_semester_id_fk FOREIGN KEY (semester_id) REFERENCES public.semesters(semester_id);


--
-- Name: course_select course_select_classes_class_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_select
    ADD CONSTRAINT course_select_classes_class_id_fk FOREIGN KEY (class_id) REFERENCES public.classes(class_id);


--
-- Name: course_select course_select_sid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_select
    ADD CONSTRAINT course_select_sid_fkey FOREIGN KEY (sid) REFERENCES public.students(sid);


--
-- Name: course_type course_type_courses_course_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course_type
    ADD CONSTRAINT course_type_courses_course_id_fk FOREIGN KEY (course_id) REFERENCES public.courses(course_id);


--
-- Name: courses courses_departments_dept_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_departments_dept_id_fk FOREIGN KEY (dept_id) REFERENCES public.departments(dept_id);


--
-- Name: majors majors_departments_dept_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.majors
    ADD CONSTRAINT majors_departments_dept_id_fk FOREIGN KEY (dept_id) REFERENCES public.departments(dept_id);


--
-- Name: students students_ad_classes_ad_class_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_ad_classes_ad_class_id_fk FOREIGN KEY (ad_class_id) REFERENCES public.ad_classes(ad_class_id);


--
-- Name: students students_majors_major_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_majors_major_id_fk FOREIGN KEY (major_id) REFERENCES public.majors(major_id);


--
-- Name: students students_teachers_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_teachers_user_id_fk FOREIGN KEY (user_id) REFERENCES public.teachers(user_id);


--
-- PostgreSQL database dump complete
--

