-- Per-user notification preferences. Server-readable (the RSVP reminder cron
-- reads it), so it can't live in the client DataStore. Own-row RLS keeps each
-- user's prefs private — unlike the world-readable users table.

CREATE TABLE notification_settings (
    user_id       UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    rsvp_reminder TEXT NOT NULL DEFAULT 'day_of'
        CHECK (rsvp_reminder IN ('none', 'day_of', 'hour_before')),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE notification_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users read own notification settings"   ON notification_settings FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "Users insert own notification settings" ON notification_settings FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "Users update own notification settings" ON notification_settings FOR UPDATE TO authenticated USING (user_id = auth.uid());
