-- Sub-items of a task, checked one by one by the new employee
-- (e.g. "Install apps on the phone" -> Teams, SSO login app, ...).
-- Deleting a task also deletes its items.
CREATE TABLE task_checklist_items (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    label VARCHAR(200) NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_checklist_items_task_id ON task_checklist_items (task_id);

-- Which items of an assignment are checked. A row exists only while the item is checked.
-- Deleting an assignment or an item also deletes its checks.
CREATE TABLE assignment_checks (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES task_assignments (id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES task_checklist_items (id) ON DELETE CASCADE,
    checked_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_assignment_checks_assignment_item UNIQUE (assignment_id, item_id)
);

CREATE INDEX idx_assignment_checks_item_id ON assignment_checks (item_id);
