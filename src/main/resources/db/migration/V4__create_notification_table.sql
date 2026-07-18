CREATE TABLE tb_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES tb_users(id),
    message TEXT NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('ISSUE_ASSIGNED', 'COMMENT_ADDED', 'PRIORITY_SET')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient ON tb_notifications(recipient_id);
CREATE INDEX idx_notifications_status ON tb_notifications(status);
