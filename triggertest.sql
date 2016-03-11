INSERT INTO registrations(studentid, coursecode, currentstatus)
  VALUES('lingot', 'MAT111', 'Waiting');

INSERT INTO registrations(studentid, coursecode, currentstatus)
  VALUES('sarlar', 'COE345', 'Registered');

INSERT INTO registrations(studentid, coursecode, currentstatus)
  VALUES('alemob', 'COE345', '');

INSERT INTO registrations(studentid, coursecode, currentstatus)
  VALUES('bergor', 'IAD678', '');

INSERT INTO registrations(studentid, coursecode, currentstatus)
  VALUES('sarlar', 'ABC123', '');

INSERT INTO registrations(studentid, coursecode, currentstatus)
  VALUES('lingot', 'MAT211', '');

INSERT INTO registrations(studentid, coursecode, currentstatus)
  VALUES('alemob', 'CSC413', '');

DELETE FROM registrations
  WHERE studentid='lingot' AND coursecode='MAT111';

DELETE FROM registrations
  WHERE studentid='alemob' AND coursecode='COE345';

DELETE FROM registrations
  WHERE studentid='bergor' AND coursecode='MAT111';