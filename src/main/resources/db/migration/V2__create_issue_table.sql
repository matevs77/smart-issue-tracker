CREATE TABLE tb_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    priority VARCHAR(20) CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    ai_confidence_score DOUBLE PRECISION CHECK (ai_confidence_score >= 0.0 AND ai_confidence_score <= 1.0),
    reporter_id UUID NOT NULL REFERENCES tb_users(id),
    assignee_id UUID REFERENCES tb_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_issues_status ON tb_issues(status);
CREATE INDEX idx_issues_reporter ON tb_issues(reporter_id);
CREATE INDEX idx_issues_assignee ON tb_issues(assignee_id);
CREATE INDEX idx_issues_created_at ON tb_issues(created_at DESC);
