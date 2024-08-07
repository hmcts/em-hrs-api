INSERT INTO folder(id, name)
VALUES ('3E3F63FB-3C7A-447B-86DA-69ED164763B0', 'folder-1'),
       ('B741E4E9-4968-47B1-B168-8EE5192CC8CC', 'folder-2'),
       ('7ACB7529-EE08-4AD4-B989-D0B3AC536578', 'folder-3');

INSERT INTO job_in_progress(id, filename, folder_id)
VALUES ('6073C487-D84C-4907-B0EC-1E4D3AEE9750', 'folder-1-file-3-1.mp4', '3E3F63FB-3C7A-447B-86DA-69ED164763B0'),
       ('68893A39-0ECE-4502-83AA-F52A8F51FD4E', 'folder-1-file-3-2.mp4', '3E3F63FB-3C7A-447B-86DA-69ED164763B0'),
       ('9FC21A81-21A0-4E84-8E49-10A13F11E1E9', 'folder-3-file-1.mp4', '7ACB7529-EE08-4AD4-B989-D0B3AC536578');

INSERT INTO hearing_recording(id, deleted, ccd_case_id, folder_id, recording_ref, ttl_set)
VALUES ('05A13771-58DF-4ABD-B62D-4A3F8DDF4286', false, 1, '3E3F63FB-3C7A-447B-86DA-69ED164763B0', 'ref11', false),
       ('66DCF6B9-2999-49CC-8175-7D67531C0F7C', false, 2, '3E3F63FB-3C7A-447B-86DA-69ED164763B0', 'ref12', false),
       ('BA5AFE3A-2AF8-42C2-849E-9A7C39363239', false, 3, 'B741E4E9-4968-47B1-B168-8EE5192CC8CC', 'ref21', false);

INSERT INTO hearing_recording_segment(id, filename, deleted, hearing_recording_id)
VALUES ('8A4FDAD2-D53E-40A2-AE88-6DF8FBD6CC1D', 'folder-1-file-1.mp4', false, '05A13771-58DF-4ABD-B62D-4A3F8DDF4286'),
       ('F143C97E-0F7B-41C2-9243-8F4C433913AA', 'folder-1-file-2-1.mp4', false, '66DCF6B9-2999-49CC-8175-7D67531C0F7C'),
       ('33B9CF46-0935-4DFF-9DA7-9DD7A0D7BA22', 'folder-1-file-2-2.mp4', false, '66DCF6B9-2999-49CC-8175-7D67531C0F7C'),
       ('68893A39-0ECE-4502-83AA-F52A8F51FD4A', 'folder-1-file-2-3.mp4', false, '66DCF6B9-2999-49CC-8175-7D67531C0F7C'),
       ('68893A39-0ECE-4502-83AA-F52A8F51FD4B', 'folder-1-file-2-4.mp4', false, '66DCF6B9-2999-49CC-8175-7D67531C0F7C'),
       ('68893A39-0ECE-4502-83AA-F52A8F51FD4C', 'folder-1-file-2-5.mp4', false, '66DCF6B9-2999-49CC-8175-7D67531C0F7C'),
       ('68893A39-0ECE-4502-83AA-F52A8F51FD4D', 'folder-1-file-2-6.mp4', false, '66DCF6B9-2999-49CC-8175-7D67531C0F7C'),
       ('5DE0838B-ADCB-417A-A1D0-180EC37E55FD', 'folder-2-file-1.mp4', false, 'BA5AFE3A-2AF8-42C2-849E-9A7C39363239');
