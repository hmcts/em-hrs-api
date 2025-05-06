CREATE INDEX IF NOT EXISTS hearing_recording_segment_hearing_recording_id ON hearing_recording_segment (hearing_recording_id);
CREATE INDEX IF NOT EXISTS hearing_recording_sharee_hearing_recording_id ON hearing_recording_sharee (hearing_recording_id);
CREATE INDEX IF NOT EXISTS audit_entry_hearing_recording_segment_id ON audit_entry (hearing_recording_segment_id);
CREATE INDEX IF NOT EXISTS audit_entry_hearing_recording_sharee_id ON audit_entry (hearing_recording_sharee_id);
CREATE INDEX IF NOT EXISTS audit_entry_hearing_recording_id ON audit_entry (hearing_recording_id);
