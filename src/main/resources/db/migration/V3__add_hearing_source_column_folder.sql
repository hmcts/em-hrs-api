
ALTER TABLE public.folder
ADD hearing_source varchar(255) NULL
DEFAULT 'CVP';

ALTER TABLE public.folder
ADD CONSTRAINT uc_foldername_hearingsource_col UNIQUE(name,hearing_source);

ALTER TABLE public.folder
DROP CONSTRAINT uc_foldername_col;

Analyse;
