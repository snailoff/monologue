CREATE TABLE "public"."knot_meta" (
    "meta" varchar NOT NULL,
    "content" varchar NOT NULL,
    "mtime" timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY ("meta")
);


CREATE TABLE "public"."knot_pieces" (
    "id" bpchar(15) NOT NULL,
    "subject" varchar NOT NULL,
    "summary" varchar,
    "content" text,
    "ctime" timestamp NOT NULL DEFAULT now(),
    "mtime" timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX piece_u_subject
ON knot_pieces(subject);

CREATE TABLE "public"."knot_tags" (
    "id" serial,
    "name" varchar NOT NULL,
    "content" text,
    "ctime" timestamp NOT NULL DEFAULT now(),
    "mtime" timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX tag_u_name
ON knot_tags(name);

CREATE TABLE "public"."link_pieces" (
    "from_piece_id" bpchar(15) NOT NULL,
    "to_piece_id" bpchar(15) NOT NULL,
    "ctime" timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY ("from_piece_id","to_piece_id")
);

CREATE TABLE "public"."link_tag_piece" (
    "tag_id" int4 NOT NULL,
    "piece_id" bpchar(15) NOT NULL,
    "ctime" timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY ("tag_id","piece_id")
);