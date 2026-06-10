CREATE TABLE review_replies (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id        UUID NOT NULL,
    parent_reply_id  UUID,
    author_user_id   VARCHAR(50) NOT NULL,
    message          TEXT NOT NULL,
    created_date     TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_date    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_review_replies_review FOREIGN KEY (review_id) REFERENCES reviews(id),
    CONSTRAINT fk_review_replies_parent FOREIGN KEY (parent_reply_id) REFERENCES review_replies(id),
    CONSTRAINT fk_review_replies_author FOREIGN KEY (author_user_id) REFERENCES APP_USER(uuid)
);

CREATE INDEX idx_review_replies_review_id ON review_replies (review_id);
