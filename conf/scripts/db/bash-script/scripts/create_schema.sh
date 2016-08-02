#!/bin/bash

psql -U $USERNAME -d $DBNAME -p $PORT -c "
--
-- PostgreSQL database dump
--

-- Dumped from database version 9.4.1
-- Dumped by pg_dump version 9.5.1

-- Started on 2016-06-23 15:19:15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

SET search_path = public, pg_catalog;

--
-- TOC entry 1281 (class 1255 OID 246275)
-- Name: audit_location(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION audit_location() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
IF (TG_OP = 'UPDATE' OR TG_OP = 'DELETE') THEN
	INSERT INTO audit_location(gid,name,start_date,end_date,protect,update_date,operation,user_id,parent_gid,location_type_id,code,code_type_id,description,gis_src_id,kml)
	VALUES(OLD.gid,OLD.name,OLD.start_date,OLD.end_date,OLD.protect,CURRENT_DATE,TG_OP,OLD.user_id,OLD.parent_gid,OLD.location_type_id,OLD.code,OLD.code_type_id,OLD.description,OLD.gis_src_id,OLD.kml);
END IF;
RETURN NEW;
END;
$$;


--
-- TOC entry 1401 (class 1255 OID 246276)
-- Name: audit_location_geometry(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION audit_location_geometry() RETURNS trigger
    LANGUAGE plpgsql
    AS $$

BEGIN
IF (TG_OP = 'UPDATE' OR TG_OP = 'DELETE') THEN
	INSERT INTO AUDIT_LOCATION_GEOMETRY(gid,multipolygon,area,operation,update_date)
	VALUES(OLD.gid,OLD.multipolygon,OLD.area,TG_OP,CURRENT_DATE);
END IF;
RETURN NEW;
END;
$$;


--
-- TOC entry 1369 (class 1255 OID 246277)
-- Name: calc_area(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION calc_area() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
NEW.area := st_area(NEW.multipolygon,true);
RETURN NEW;
END;	
$$;


--
-- TOC entry 1379 (class 1255 OID 246278)
-- Name: calc_area_envelope(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION calc_area_envelope() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
NEW.area := st_area(NEW.multipolygon,true);
NEW.envelope := st_envelope(NEW.multipolygon);
RETURN NEW;
END;	
$$;


--
-- TOC entry 1282 (class 1255 OID 309980)
-- Name: calc_area_envelope_reppoint(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION calc_area_envelope_reppoint() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
--new_geom Geometry(multipolygon,4326);
validity text;
BEGIN
validity := ST_isValidReason(NEW.multipolygon);
IF validity <> 'Valid Geometry' THEN
	RAISE EXCEPTION 'Invalid Geometry: %', validity || ' ,gid= ' || new.gid::text;
END IF;
NEW.area := st_area(st_transform(NEW.multipolygon,922));
NEW.envelope := st_envelope(NEW.multipolygon);
NEW.update_date := CURRENT_DATE;
NEW.rep_point := (
with sub_geoms as(
	select (st_dump(NEW.multipolygon)).geom as sub_geom
	),
areas as (
	select sub_geom ,st_area(st_transform(sub_geom,922)) as area from sub_geoms
	)
select st_pointOnSurface(sub_geom) from areas
where area = (select max(area) from areas)
limit 1
);
RETURN NEW;
-- EXCEPTION WHEN OTHERS THEN
-- DO NOTHING
END;	
$$;


--
-- TOC entry 1389 (class 1255 OID 246279)
-- Name: internalid_formaltable_name_lookup(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION internalid_formaltable_name_lookup() RETURNS TABLE(count bigint)
    LANGUAGE plpgsql
    AS $$
DECLARE
   formal_table text;
BEGIN
   FOR formal_table IN
      SELECT quote_ident(tablename)
      FROM   pg_tables
      WHERE  schemaname = 'public'
      AND    tablename  NOT LIKE 'pg_%'
   LOOP
	raise notice 'tablename %', quote_ident(formal_table);--quote_ident(rec.tablename);

      RETURN QUERY EXECUTE
      'SELECT count(*)
       FROM    ' || formal_table || ' ;' ;
   END LOOP;
END
$$;


--
-- TOC entry 453 (class 1255 OID 339196)
-- Name: placename_simplify(text); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION placename_simplify(text) RETURNS text
    LANGUAGE plpgsql
    AS $_$
begin
	return trim(lower(unaccent(replace($1,'_',' '))));
end;
$_$;


--
-- TOC entry 1402 (class 1255 OID 246280)
-- Name: prevent_duplicate_location(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION prevent_duplicate_location() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
id integer;
BEGIN
id := (SELECT gid FROM LOCATION 
	WHERE 
            NEW.gid <> gid --No self comparison on updates
            AND
            NEW.name ILIKE name
            AND
            NEW.location_type_id = location_type_id
            AND
            NEW.parent_gid IS NOT DISTINCT FROM parent_gid
            AND
            (
            NEW.start_date BETWEEN start_date AND LEAST(end_date,CURRENT_DATE)
            OR
            LEAST(NEW.end_date,CURRENT_DATE) BETWEEN start_date AND LEAST(end_date,CURRENT_DATE)
            )
        LIMIT 1    
	);
IF (id IS NOT NULL)  
                THEN
		RAISE 'duplicate_violation: gid=%', id USING ERRCODE = 'unique_violation';
                RETURN NULL;
  -- DO NOTHING;
  ELSE
  RETURN NEW;
END IF;
END;
$$;


--
-- TOC entry 1400 (class 1255 OID 246281)
-- Name: text_search(character varying); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION text_search(character varying) RETURNS TABLE(name1 character varying)
    LANGUAGE plpgsql
    AS $_$
-- DECLARE
--    formal_table text;
BEGIN
   RETURN QUERY EXECUTE
   'select name from location
    where name ilike' || E'\'%' || $1 || E'%\'';
END
$_$;


--
-- TOC entry 1384 (class 1255 OID 309988)
-- Name: unaccent_immutable(name); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION unaccent_immutable(text name) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $$

BEGIN
RETURN unaccent(name);
END;
$$;


--
-- TOC entry 1385 (class 1255 OID 309982)
-- Name: unaccent_immutable(text); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION unaccent_immutable(name text) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $$

BEGIN
RETURN unaccent(name);
END;
$$;


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 191 (class 1259 OID 246282)
-- Name: alt_code; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE alt_code (
    id bigint NOT NULL,
    code character varying(255),
    code_type_id bigint,
    gid bigint
);


--
-- TOC entry 192 (class 1259 OID 246285)
-- Name: alt_code_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE alt_code_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4510 (class 0 OID 0)
-- Dependencies: 192
-- Name: alt_code_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE alt_code_id_seq OWNED BY alt_code.id;


--
-- TOC entry 219 (class 1259 OID 309886)
-- Name: alt_name; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE alt_name (
    id bigint NOT NULL,
    description character varying(4000),
    lang character varying(3),
    name character varying(255),
    gis_src_id bigint,
    gid bigint NOT NULL
);


--
-- TOC entry 218 (class 1259 OID 309884)
-- Name: alt_name_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE alt_name_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4511 (class 0 OID 0)
-- Dependencies: 218
-- Name: alt_name_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE alt_name_id_seq OWNED BY alt_name.id;


--
-- TOC entry 193 (class 1259 OID 246287)
-- Name: audit_location; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE audit_location (
    id bigint NOT NULL,
    code character varying(255),
    gid_path character varying(255),
    description character varying(2500),
    end_date date,
    name character varying(255),
    protect boolean,
    start_date date,
    update_date date NOT NULL,
    user_id bigint,
    gid bigint,
    operation character varying(255),
    parent_gid bigint,
    code_type_id bigint NOT NULL,
    gis_src_id bigint NOT NULL,
    location_type_id integer NOT NULL,
    kml text,
    area double precision
);


--
-- TOC entry 194 (class 1259 OID 246293)
-- Name: audit_location_geometry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE audit_location_geometry (
    id bigint NOT NULL,
    gid bigint,
    multipolygon geometry,
    operation character varying(255),
    update_date date,
    area double precision
);


--
-- TOC entry 195 (class 1259 OID 246299)
-- Name: audit_location_geometry_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE audit_location_geometry_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4512 (class 0 OID 0)
-- Dependencies: 195
-- Name: audit_location_geometry_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE audit_location_geometry_id_seq OWNED BY audit_location_geometry.id;


--
-- TOC entry 196 (class 1259 OID 246301)
-- Name: audit_location_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE audit_location_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4513 (class 0 OID 0)
-- Dependencies: 196
-- Name: audit_location_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE audit_location_id_seq OWNED BY audit_location.id;


--
-- TOC entry 197 (class 1259 OID 246303)
-- Name: audit_location_location_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE audit_location_location_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4514 (class 0 OID 0)
-- Dependencies: 197
-- Name: audit_location_location_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE audit_location_location_type_id_seq OWNED BY audit_location.location_type_id;


--
-- TOC entry 200 (class 1259 OID 246313)
-- Name: code_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE code_type (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- TOC entry 201 (class 1259 OID 246316)
-- Name: code_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE code_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4515 (class 0 OID 0)
-- Dependencies: 201
-- Name: code_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE code_type_id_seq OWNED BY code_type.id;


--
-- TOC entry 206 (class 1259 OID 246334)
-- Name: gis_src; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE gis_src (
    id bigint NOT NULL,
    url character varying(255)
);


--
-- TOC entry 207 (class 1259 OID 246337)
-- Name: gis_src_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE gis_src_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4516 (class 0 OID 0)
-- Dependencies: 207
-- Name: gis_src_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE gis_src_id_seq OWNED BY gis_src.id;


--
-- TOC entry 208 (class 1259 OID 246345)
-- Name: location; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE location (
    gid bigint NOT NULL,
    code character varying(255),
    description character varying(2500),
    end_date date,
    name character varying(255),
    protect boolean,
    start_date date NOT NULL,
    update_date date NOT NULL,
    user_id bigint,
    code_type_id bigint NOT NULL,
    gis_src_id bigint NOT NULL,
    location_type_id integer NOT NULL,
    parent_gid bigint,
    kml text,
    CONSTRAINT date_check CHECK ((end_date >= start_date))
);


--
-- TOC entry 209 (class 1259 OID 246352)
-- Name: location_definition; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE location_definition (
    gid bigint NOT NULL,
    included_gid bigint NOT NULL
);


--
-- TOC entry 210 (class 1259 OID 246355)
-- Name: location_geometry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE location_geometry (
    gid bigint NOT NULL,
    multipolygon geometry(Geometry,4326),
    area double precision,
    update_date date,
    envelope geometry,
    rep_point geometry
);


--
-- TOC entry 211 (class 1259 OID 246361)
-- Name: location_gid_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE location_gid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4517 (class 0 OID 0)
-- Dependencies: 211
-- Name: location_gid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE location_gid_seq OWNED BY location.gid;


--
-- TOC entry 212 (class 1259 OID 246363)
-- Name: location_location_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE location_location_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4518 (class 0 OID 0)
-- Dependencies: 212
-- Name: location_location_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE location_location_type_id_seq OWNED BY location.location_type_id;


--
-- TOC entry 213 (class 1259 OID 246365)
-- Name: location_low_resolution_geometry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE location_low_resolution_geometry (
    gid bigint NOT NULL,
    multipolygon geometry,
    area double precision,
    update_date date,
    rep_point geometry
);


--
-- TOC entry 222 (class 1259 OID 309957)
-- Name: location_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE location_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 214 (class 1259 OID 246371)
-- Name: location_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE location_type (
    id bigint DEFAULT nextval('location_type_id_seq'::regclass) NOT NULL,
    name character varying(255),
    super_type_id integer NOT NULL,
    user_definable boolean DEFAULT false NOT NULL,
    composed_of_id integer
);


--
-- TOC entry 215 (class 1259 OID 246391)
-- Name: related_location; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE related_location (
    gid1 bigint NOT NULL,
    gid2 bigint NOT NULL
);


--
-- TOC entry 216 (class 1259 OID 246394)
-- Name: super_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE super_type (
    id integer NOT NULL,
    name character varying(255),
    user_definable boolean DEFAULT false NOT NULL
);


--
-- TOC entry 217 (class 1259 OID 246398)
-- Name: super_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE super_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4519 (class 0 OID 0)
-- Dependencies: 217
-- Name: super_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE super_type_id_seq OWNED BY super_type.id;


--
-- TOC entry 4325 (class 2604 OID 246400)
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_code ALTER COLUMN id SET DEFAULT nextval('alt_code_id_seq'::regclass);


--
-- TOC entry 4338 (class 2604 OID 309889)
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_name ALTER COLUMN id SET DEFAULT nextval('alt_name_id_seq'::regclass);


--
-- TOC entry 4326 (class 2604 OID 246401)
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location ALTER COLUMN id SET DEFAULT nextval('audit_location_id_seq'::regclass);


--
-- TOC entry 4327 (class 2604 OID 246402)
-- Name: location_type_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location ALTER COLUMN location_type_id SET DEFAULT nextval('audit_location_location_type_id_seq'::regclass);


--
-- TOC entry 4328 (class 2604 OID 246403)
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location_geometry ALTER COLUMN id SET DEFAULT nextval('audit_location_geometry_id_seq'::regclass);


--
-- TOC entry 4329 (class 2604 OID 246405)
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY code_type ALTER COLUMN id SET DEFAULT nextval('code_type_id_seq'::regclass);


--
-- TOC entry 4330 (class 2604 OID 246408)
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY gis_src ALTER COLUMN id SET DEFAULT nextval('gis_src_id_seq'::regclass);


--
-- TOC entry 4331 (class 2604 OID 246409)
-- Name: gid; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY location ALTER COLUMN gid SET DEFAULT nextval('location_gid_seq'::regclass);


--
-- TOC entry 4332 (class 2604 OID 246410)
-- Name: location_type_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY location ALTER COLUMN location_type_id SET DEFAULT nextval('location_location_type_id_seq'::regclass);


--
-- TOC entry 4337 (class 2604 OID 246415)
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY super_type ALTER COLUMN id SET DEFAULT nextval('super_type_id_seq'::regclass);


--
-- TOC entry 4340 (class 2606 OID 309748)
-- Name: alt_code5_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_code
    ADD CONSTRAINT alt_code5_pkey PRIMARY KEY (id);


--
-- TOC entry 4366 (class 2606 OID 309894)
-- Name: alt_name_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_name
    ADD CONSTRAINT alt_name_pkey PRIMARY KEY (id);


--
-- TOC entry 4345 (class 2606 OID 309750)
-- Name: audit_location_geometry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location_geometry
    ADD CONSTRAINT audit_location_geometry_pkey PRIMARY KEY (id);


--
-- TOC entry 4343 (class 2606 OID 309752)
-- Name: audit_location_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location
    ADD CONSTRAINT audit_location_pkey PRIMARY KEY (id);


--
-- TOC entry 4347 (class 2606 OID 309756)
-- Name: code_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY code_type
    ADD CONSTRAINT code_type_pkey PRIMARY KEY (id);


--
-- TOC entry 4349 (class 2606 OID 309762)
-- Name: gis_src_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY gis_src
    ADD CONSTRAINT gis_src_pkey PRIMARY KEY (id);


--
-- TOC entry 4356 (class 2606 OID 309764)
-- Name: location_geometry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location_geometry
    ADD CONSTRAINT location_geometry_pkey PRIMARY KEY (gid);


--
-- TOC entry 4359 (class 2606 OID 309766)
-- Name: location_optimized_geometry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location_low_resolution_geometry
    ADD CONSTRAINT location_optimized_geometry_pkey PRIMARY KEY (gid);


--
-- TOC entry 4353 (class 2606 OID 309768)
-- Name: location_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location
    ADD CONSTRAINT location_pkey PRIMARY KEY (gid);


--
-- TOC entry 4361 (class 2606 OID 309960)
-- Name: location_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location_type
    ADD CONSTRAINT location_type_pkey PRIMARY KEY (id);


--
-- TOC entry 4363 (class 2606 OID 309774)
-- Name: super_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY super_type
    ADD CONSTRAINT super_type_pkey PRIMARY KEY (id);


--
-- TOC entry 4341 (class 1259 OID 387440)
-- Name: alt_code_tsvector_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX alt_code_tsvector_idx ON alt_code USING gin (to_tsvector('simple'::regconfig, (code)::text));


--
-- TOC entry 4364 (class 1259 OID 309993)
-- Name: alt_name_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX alt_name_idx ON alt_name USING gin (to_tsvector('simple'::regconfig, (name)::text));


--
-- TOC entry 4367 (class 1259 OID 309992)
-- Name: alt_name_unaccent_immutable_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX alt_name_unaccent_immutable_idx ON alt_name USING btree (unaccent_immutable((name)::text));


--
-- TOC entry 4350 (class 1259 OID 309928)
-- Name: location_name_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX location_name_idx ON location USING gin (to_tsvector('simple'::regconfig, (name)::text));


--
-- TOC entry 4351 (class 1259 OID 309994)
-- Name: location_name_unaccent_immutable_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX location_name_unaccent_immutable_idx ON location USING btree (unaccent_immutable((name)::text));


--
-- TOC entry 4354 (class 1259 OID 309778)
-- Name: location_type_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX location_type_id ON location USING btree (location_type_id);


--
-- TOC entry 4357 (class 1259 OID 309779)
-- Name: multipolygon_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX multipolygon_index ON location_geometry USING gist (multipolygon);


--
-- TOC entry 4388 (class 2620 OID 309981)
-- Name: calc_area_envelope_reppoint; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER calc_area_envelope_reppoint BEFORE INSERT OR UPDATE ON location_geometry FOR EACH ROW EXECUTE PROCEDURE calc_area_envelope_reppoint();


--
-- TOC entry 4385 (class 2620 OID 309977)
-- Name: location_audit; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER location_audit AFTER DELETE OR UPDATE ON location FOR EACH ROW EXECUTE PROCEDURE audit_location();


--
-- TOC entry 4387 (class 2620 OID 309978)
-- Name: location_geometry_audit; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER location_geometry_audit AFTER DELETE OR UPDATE ON location_geometry FOR EACH ROW EXECUTE PROCEDURE audit_location_geometry();


--
-- TOC entry 4386 (class 2620 OID 309979)
-- Name: location_prevent_duplicates; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER location_prevent_duplicates BEFORE INSERT OR UPDATE ON location FOR EACH ROW EXECUTE PROCEDURE prevent_duplicate_location();


--
-- TOC entry 4373 (class 2606 OID 309784)
-- Name: fk_1xy18vee7xaabypaj3n5jqee; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location
    ADD CONSTRAINT fk_1xy18vee7xaabypaj3n5jqee FOREIGN KEY (code_type_id) REFERENCES code_type(id);


--
-- TOC entry 4377 (class 2606 OID 309789)
-- Name: fk_2leen0ff20fa7il8bbq9mhm2l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location_definition
    ADD CONSTRAINT fk_2leen0ff20fa7il8bbq9mhm2l FOREIGN KEY (gid) REFERENCES location(gid);


--
-- TOC entry 4381 (class 2606 OID 309794)
-- Name: fk_51gbbyih847jfqr0momn1g9af; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY related_location
    ADD CONSTRAINT fk_51gbbyih847jfqr0momn1g9af FOREIGN KEY (gid2) REFERENCES location(gid);


--
-- TOC entry 4382 (class 2606 OID 309799)
-- Name: fk_5y6ym2e7nglngckppvle76u2v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY related_location
    ADD CONSTRAINT fk_5y6ym2e7nglngckppvle76u2v FOREIGN KEY (gid1) REFERENCES location(gid);


--
-- TOC entry 4370 (class 2606 OID 309804)
-- Name: fk_6gfgc1ctoxegveu867ft99m47; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location
    ADD CONSTRAINT fk_6gfgc1ctoxegveu867ft99m47 FOREIGN KEY (code_type_id) REFERENCES code_type(id);


--
-- TOC entry 4372 (class 2606 OID 309966)
-- Name: fk_98vx00008pg9t1qt56o2pq0c5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location
    ADD CONSTRAINT fk_98vx00008pg9t1qt56o2pq0c5 FOREIGN KEY (location_type_id) REFERENCES location_type(id);


--
-- TOC entry 4376 (class 2606 OID 309971)
-- Name: fk_a37gs5po1sy9eyx3ahh4q319h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location
    ADD CONSTRAINT fk_a37gs5po1sy9eyx3ahh4q319h FOREIGN KEY (location_type_id) REFERENCES location_type(id);


--
-- TOC entry 4374 (class 2606 OID 309829)
-- Name: fk_ccwo5xt7c67ngolc8x52ktb4f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location
    ADD CONSTRAINT fk_ccwo5xt7c67ngolc8x52ktb4f FOREIGN KEY (gis_src_id) REFERENCES gis_src(id);


--
-- TOC entry 4368 (class 2606 OID 309834)
-- Name: fk_ij5xyt7knxw3afjwy166vtag5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_code
    ADD CONSTRAINT fk_ij5xyt7knxw3afjwy166vtag5 FOREIGN KEY (code_type_id) REFERENCES code_type(id);


--
-- TOC entry 4383 (class 2606 OID 309895)
-- Name: fk_ikm2wd63nt6wwbhlf2woecdd3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_name
    ADD CONSTRAINT fk_ikm2wd63nt6wwbhlf2woecdd3 FOREIGN KEY (gis_src_id) REFERENCES gis_src(id);


--
-- TOC entry 4371 (class 2606 OID 309839)
-- Name: fk_ki48nmbdcuh5m7ffobcsl7b57; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY audit_location
    ADD CONSTRAINT fk_ki48nmbdcuh5m7ffobcsl7b57 FOREIGN KEY (gis_src_id) REFERENCES gis_src(id);


--
-- TOC entry 4384 (class 2606 OID 309900)
-- Name: fk_m089xv2vv9j4pedgn67xofepx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_name
    ADD CONSTRAINT fk_m089xv2vv9j4pedgn67xofepx FOREIGN KEY (gid) REFERENCES location(gid);


--
-- TOC entry 4378 (class 2606 OID 309844)
-- Name: fk_pg779skam9gqirp7cudovman3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location_definition
    ADD CONSTRAINT fk_pg779skam9gqirp7cudovman3 FOREIGN KEY (included_gid) REFERENCES location(gid);


--
-- TOC entry 4375 (class 2606 OID 309849)
-- Name: fk_rulncca31o3ukf0sc9a1m8bx0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location
    ADD CONSTRAINT fk_rulncca31o3ukf0sc9a1m8bx0 FOREIGN KEY (parent_gid) REFERENCES location(gid);


--
-- TOC entry 4369 (class 2606 OID 309854)
-- Name: fk_viafcrvev6kqmyjkfttipiwm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY alt_code
    ADD CONSTRAINT fk_viafcrvev6kqmyjkfttipiwm FOREIGN KEY (gid) REFERENCES location(gid);


--
-- TOC entry 4380 (class 2606 OID 309961)
-- Name: location_type_composed_of_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location_type
    ADD CONSTRAINT location_type_composed_of_id_fkey FOREIGN KEY (composed_of_id) REFERENCES location_type(id);


--
-- TOC entry 4379 (class 2606 OID 309864)
-- Name: location_type_super_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY location_type
    ADD CONSTRAINT location_type_super_type_id_fkey FOREIGN KEY (super_type_id) REFERENCES super_type(id);


-- Completed on 2016-06-23 15:19:16

--
-- PostgreSQL database dump complete
--

"