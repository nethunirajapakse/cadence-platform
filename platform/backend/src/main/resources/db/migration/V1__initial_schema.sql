--
-- Name: achievements; Type: TABLE; Schema: public
--

CREATE TABLE public.achievements (
                                     achievement_id uuid NOT NULL,
                                     created_at timestamp(6) without time zone NOT NULL,
                                     description text NOT NULL,
                                     is_key_achievement boolean NOT NULL,
                                     updated_at timestamp(6) without time zone NOT NULL,
                                     report_id uuid NOT NULL
);

--
-- Name: blockers; Type: TABLE; Schema: public
--

CREATE TABLE public.blockers (
                                 blocker_id uuid NOT NULL,
                                 created_at timestamp(6) without time zone NOT NULL,
                                 description text NOT NULL,
                                 is_key_issue boolean NOT NULL,
                                 updated_at timestamp(6) without time zone NOT NULL,
                                 report_id uuid NOT NULL
);

--
-- Name: next_week_tasks; Type: TABLE; Schema: public
--

CREATE TABLE public.next_week_tasks (
                                        next_task_id uuid NOT NULL,
                                        created_at timestamp(6) without time zone NOT NULL,
                                        priority character varying(10),
                                        task_description character varying(300) NOT NULL,
                                        updated_at timestamp(6) without time zone NOT NULL,
                                        report_id uuid NOT NULL,
                                        CONSTRAINT next_week_tasks_priority_check CHECK (((priority)::text = ANY ((ARRAY['HIGH'::character varying, 'MEDIUM'::character varying, 'LOW'::character varying])::text[])))
);

--
-- Name: projects; Type: TABLE; Schema: public
--

CREATE TABLE public.projects (
                                 project_id uuid NOT NULL,
                                 created_at timestamp(6) without time zone NOT NULL,
                                 description character varying(500),
                                 name character varying(120) NOT NULL,
                                 updated_at timestamp(6) without time zone NOT NULL
);

--
-- Name: report_notes_links; Type: TABLE; Schema: public
--

CREATE TABLE public.report_notes_links (
                                           note_link_id uuid NOT NULL,
                                           content text NOT NULL,
                                           created_at timestamp(6) without time zone NOT NULL,
                                           type character varying(10) NOT NULL,
                                           updated_at timestamp(6) without time zone NOT NULL,
                                           report_id uuid NOT NULL,
                                           CONSTRAINT report_notes_links_type_check CHECK (((type)::text = ANY ((ARRAY['NOTE'::character varying, 'LINK'::character varying])::text[])))
);

--
-- Name: report_tasks; Type: TABLE; Schema: public
--

CREATE TABLE public.report_tasks (
                                     task_id uuid NOT NULL,
                                     actual_pct integer,
                                     deliverable character varying(300),
                                     planned_pct integer,
                                     priority character varying(10) NOT NULL,
                                     status character varying(20) NOT NULL,
                                     task_name character varying(200) NOT NULL,
                                     time_planned numeric(5,2),
                                     time_spent numeric(5,2),
                                     report_id uuid NOT NULL,
                                     CONSTRAINT report_tasks_priority_check CHECK (((priority)::text = ANY ((ARRAY['HIGH'::character varying, 'MEDIUM'::character varying, 'LOW'::character varying])::text[]))),
    CONSTRAINT report_tasks_status_check CHECK (((status)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'IN_PROGRESS'::character varying, 'DONE'::character varying])::text[])))
);

--
-- Name: report_versions; Type: TABLE; Schema: public
--

CREATE TABLE public.report_versions (
                                        version_id uuid NOT NULL,
                                        comment text,
                                        comment_edited boolean DEFAULT false NOT NULL,
                                        comment_posted_at timestamp(6) without time zone,
                                        content_snapshot jsonb NOT NULL,
                                        is_current boolean NOT NULL,
                                        submitted_at timestamp(6) without time zone NOT NULL,
                                        version_number integer NOT NULL,
                                        report_id uuid NOT NULL
);

--
-- Name: roles; Type: TABLE; Schema: public
--

CREATE TABLE public.roles (
                              role_id uuid NOT NULL,
                              description character varying(255),
                              role_name character varying(30) NOT NULL,
                              CONSTRAINT roles_role_name_check CHECK (((role_name)::text = ANY ((ARRAY['TEAM_MEMBER'::character varying, 'MANAGER'::character varying])::text[])))
);

--
-- Name: time_logs; Type: TABLE; Schema: public
--

CREATE TABLE public.time_logs (
                                  time_log_id uuid NOT NULL,
                                  created_at timestamp(6) without time zone NOT NULL,
                                  hours numeric(5,2) NOT NULL,
                                  task_type character varying(30) NOT NULL,
                                  updated_at timestamp(6) without time zone NOT NULL,
                                  report_id uuid NOT NULL,
                                  CONSTRAINT time_logs_task_type_check CHECK (((task_type)::text = ANY ((ARRAY['DEVELOPMENT'::character varying, 'TESTING'::character varying, 'MEETINGS'::character varying, 'DOCUMENTATION'::character varying, 'OTHER'::character varying])::text[])))
);

--
-- Name: users; Type: TABLE; Schema: public
--

CREATE TABLE public.users (
                              user_id uuid NOT NULL,
                              is_active boolean NOT NULL,
                              created_at timestamp(6) without time zone NOT NULL,
                              email character varying(180) NOT NULL,
                              name character varying(120) NOT NULL,
                              password_hash character varying(255) NOT NULL,
                              updated_at timestamp(6) without time zone NOT NULL,
                              role_id uuid NOT NULL
);

--
-- Name: weekly_reports; Type: TABLE; Schema: public
--

CREATE TABLE public.weekly_reports (
                                       report_id uuid NOT NULL,
                                       approved_at timestamp(6) without time zone,
                                       created_at timestamp(6) without time zone NOT NULL,
                                       manager_comment text,
                                       notes text,
                                       status character varying(20) NOT NULL,
                                       submitted_at timestamp(6) without time zone,
                                       updated_at timestamp(6) without time zone NOT NULL,
                                       week_end_date date NOT NULL,
                                       week_start_date date NOT NULL,
                                       project_id uuid NOT NULL,
                                       user_id uuid NOT NULL,
                                       CONSTRAINT weekly_reports_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'NEEDS_CORRECTION'::character varying, 'APPROVED'::character varying])::text[])))
);

--
-- Primary key / unique constraints
--

ALTER TABLE ONLY public.achievements
    ADD CONSTRAINT achievements_pkey PRIMARY KEY (achievement_id);

ALTER TABLE ONLY public.blockers
    ADD CONSTRAINT blockers_pkey PRIMARY KEY (blocker_id);

ALTER TABLE ONLY public.next_week_tasks
    ADD CONSTRAINT next_week_tasks_pkey PRIMARY KEY (next_task_id);

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (project_id);

ALTER TABLE ONLY public.report_notes_links
    ADD CONSTRAINT report_notes_links_pkey PRIMARY KEY (note_link_id);

ALTER TABLE ONLY public.report_tasks
    ADD CONSTRAINT report_tasks_pkey PRIMARY KEY (task_id);

ALTER TABLE ONLY public.report_versions
    ADD CONSTRAINT report_versions_pkey PRIMARY KEY (version_id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);

ALTER TABLE ONLY public.time_logs
    ADD CONSTRAINT time_logs_pkey PRIMARY KEY (time_log_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT uk716hgxp60ym1lifrdgp67xt5k UNIQUE (role_name);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.weekly_reports
    ADD CONSTRAINT weekly_reports_pkey PRIMARY KEY (report_id);

--
-- Foreign key constraints
--

ALTER TABLE ONLY public.report_notes_links
    ADD CONSTRAINT fk1dlmlp42o2yvko1vrf44ggs05 FOREIGN KEY (report_id) REFERENCES public.weekly_reports(report_id);

ALTER TABLE ONLY public.report_versions
    ADD CONSTRAINT fk5a2f0sug9l781uhofs1a9eg2w FOREIGN KEY (report_id) REFERENCES public.weekly_reports(report_id);

ALTER TABLE ONLY public.blockers
    ADD CONSTRAINT fk92r01ipb0c2x21ffr7pc5ahpi FOREIGN KEY (report_id) REFERENCES public.weekly_reports(report_id);

ALTER TABLE ONLY public.weekly_reports
    ADD CONSTRAINT fkfrxk8bl23s1300lluwkclrydo FOREIGN KEY (project_id) REFERENCES public.projects(project_id);

ALTER TABLE ONLY public.report_tasks
    ADD CONSTRAINT fkg2es50m0cuofn06ea7t0dgvhv FOREIGN KEY (report_id) REFERENCES public.weekly_reports(report_id);

ALTER TABLE ONLY public.next_week_tasks
    ADD CONSTRAINT fkha9kp7uoheqo8tqj5jjl18pgh FOREIGN KEY (report_id) REFERENCES public.weekly_reports(report_id);

ALTER TABLE ONLY public.achievements
    ADD CONSTRAINT fkp2rbxihvjeb4mg5eoudgc8743 FOREIGN KEY (report_id) REFERENCES public.weekly_reports(report_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkp56c1712k691lhsyewcssf40f FOREIGN KEY (role_id) REFERENCES public.roles(role_id);

ALTER TABLE ONLY public.weekly_reports
    ADD CONSTRAINT fkqmbqcc7qb4mfr8wbc2xecqwin FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.time_logs
    ADD CONSTRAINT fkqt4rghp3mtiyjxufdt6ar07xj FOREIGN KEY (report_id) REFERENCES public.weekly_reports(report_id);