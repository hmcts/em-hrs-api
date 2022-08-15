
CREATE TABLE public.databasechangelog (
	id varchar(255) NOT NULL,
	author varchar(255) NOT NULL,
	filename varchar(255) NOT NULL,
	dateexecuted timestamp NOT NULL,
	orderexecuted int4 NOT NULL,
	exectype varchar(10) NOT NULL,
	md5sum varchar(35) NULL,
	description varchar(255) NULL,
	"comments" varchar(255) NULL,
	tag varchar(255) NULL,
	liquibase varchar(20) NULL,
	contexts varchar(255) NULL,
	labels varchar(255) NULL,
	deployment_id varchar(10) NULL
);

CREATE TABLE public.databasechangeloglock (
	id int4 NOT NULL,
	"locked" bool NOT NULL,
	lockgranted timestamp NULL,
	lockedby varchar(255) NULL,
	CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id)
);

CREATE TABLE public.folder (
	id uuid NOT NULL,
	created_by varchar(255) NULL,
	created_on timestamp NULL,
	last_modified_by varchar(255) NULL,
	modified_on timestamp NULL,
	name varchar(255) NULL,
	CONSTRAINT "folderPK" PRIMARY KEY (id),
	CONSTRAINT uc_foldername_col UNIQUE (name)
);

CREATE TABLE public.shedlock (
	name varchar(64) NOT NULL,
	lock_until timestamp(3) NULL,
	locked_at timestamp(3) NULL,
	locked_by varchar(255) NULL,
	CONSTRAINT shedlock_pkey PRIMARY KEY (name)
);

CREATE TABLE public.hearing_recording (
	id uuid NOT NULL,
	created_by varchar(255) NULL,
	created_by_service varchar(255) NULL,
	created_on timestamp NULL,
	deleted bool NOT NULL,
	hearing_source varchar(255) NULL,
	jurisdiction_code varchar(255) NULL,
	last_modified_by varchar(255) NULL,
	last_modified_by_service varchar(255) NULL,
	modified_on timestamp NULL,
	service_code varchar(255) NULL,
	ttl timestamp NULL,
	folder_id uuid NULL,
	ccd_case_id int8 NULL,
	case_ref varchar(255) NULL,
	hearing_location_code varchar(255) NULL,
	hearing_room_ref varchar(255) NULL,
	recording_ref varchar(255) NULL,
	CONSTRAINT "UKi1x8kh9td60heuhk1frv8gvck" UNIQUE (folder_id, recording_ref),
	CONSTRAINT "hearing_recordingPK" PRIMARY KEY (id),
	CONSTRAINT uc_hearing_recordingccd_case_id_col UNIQUE (ccd_case_id),
	CONSTRAINT "FK7pidb4lyq06pv6h7lf4r03r7o" FOREIGN KEY (folder_id) REFERENCES public.folder(id)
);

CREATE TABLE public.hearing_recording_metadata (
	hearing_recording_metadata_id uuid NOT NULL,
	value varchar(255) NULL,
	name varchar(255) NOT NULL,
	CONSTRAINT hearing_recording_metadata_pkey PRIMARY KEY (hearing_recording_metadata_id, name),
	CONSTRAINT "FK43w518f0nih5s1nhufcidmr6m" FOREIGN KEY (hearing_recording_metadata_id) REFERENCES public.hearing_recording(id)
);

CREATE TABLE public.hearing_recording_segment (
	id uuid NOT NULL,
	blob_uuid varchar(255) NULL,
	created_by varchar(255) NULL,
	created_by_service varchar(255) NULL,
	created_on timestamp NULL,
	deleted bool NOT NULL,
	file_extension varchar(255) NULL,
	file_md5checksum varchar(255) NULL,
	file_size_mb numeric(19, 2) NULL,
	ingestion_file_source_uri varchar(255) NULL,
	last_modified_by varchar(255) NULL,
	last_modified_by_service varchar(255) NULL,
	modified_on timestamp NULL,
	recording_length_mins int4 NULL,
	recording_segment int4 NULL,
	hearing_recording_id uuid NULL,
	filename varchar(255) NULL,
	CONSTRAINT "hearing_recording_segmentPK" PRIMARY KEY (id),
	CONSTRAINT uc_hearing_recording_segmentfilename_col UNIQUE (filename),
	CONSTRAINT "FKllxdetldfh969pd2p21w30edt" FOREIGN KEY (hearing_recording_id) REFERENCES public.hearing_recording(id)
);

CREATE TABLE public.hearing_recording_sharee (
	id uuid NOT NULL,
	shared_by_ref varchar(255) NULL,
	shared_on timestamp NULL,
	sharee_email varchar(255) NOT NULL,
	hearing_recording_id uuid NULL,
	CONSTRAINT "hearing_recording_shareePK" PRIMARY KEY (id),
	CONSTRAINT "FK40is8ijermb4qs9kw9t6h1op5" FOREIGN KEY (hearing_recording_id) REFERENCES public.hearing_recording(id)
);

CREATE TABLE public.job_in_progress (
	id uuid NOT NULL,
	created_on timestamp NULL,
	filename varchar(255) NULL,
	folder_id uuid NULL,
	CONSTRAINT "job_in_progressPK" PRIMARY KEY (id),
	CONSTRAINT "FKpcv3q6pve7gmpbnl3f3cyxlmn" FOREIGN KEY (folder_id) REFERENCES public.folder(id)
);

CREATE TABLE public.audit_entry (
	"type" varchar(31) NOT NULL,
	id uuid NOT NULL,
	"action" varchar(255) NOT NULL,
	service_name varchar(255) NOT NULL,
	username varchar(255) NULL,
	hearing_recording_id uuid NULL,
	hearing_recording_segment_id uuid NULL,
	event_date_time timestamp NOT NULL,
	ip_address varchar(255) NULL,
	hearing_recording_sharee_id uuid NULL,
	case_id int8 NULL,
	CONSTRAINT "audit_entryPK" PRIMARY KEY (id),
	CONSTRAINT "FK11roef3nv0wewk2yswnogf0nx" FOREIGN KEY (hearing_recording_sharee_id) REFERENCES public.hearing_recording_sharee(id),
	CONSTRAINT "FK7wnscpmt7mpo06iv829l35m0j" FOREIGN KEY (hearing_recording_segment_id) REFERENCES public.hearing_recording_segment(id),
	CONSTRAINT "FKqtlr65hik6rxjqlcsao05pbai" FOREIGN KEY (hearing_recording_id) REFERENCES public.hearing_recording(id)
);
