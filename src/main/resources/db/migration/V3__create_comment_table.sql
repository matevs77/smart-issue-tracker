CREATE TABLE tb_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES tb_issues(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES tb_users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_issue ON tb_comments(issue_id);
CREATE INDEX idx_comments_author ON tb_comments(author_id);
