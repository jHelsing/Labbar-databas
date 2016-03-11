CREATE OR REPLACE FUNCTION registration_function() RETURNS trigger AS $register_trigger$  
  declare
    alreadyregistered integer;
    nomeetrequirements integer;
    amount integer;
    positioninqueue integer;
    coursecapacity integer;
  
  BEGIN
    SELECT COUNT(studentid) into alreadyregistered FROM registrations
    WHERE NEW.studentid = registrations.studentid AND
            NEW.coursecode = registrations.coursecode;

    IF (alreadyregistered > 0) THEN
      RAISE USING MESSAGE = 'The student is already on the list';
      RETURN NULL;
    END IF;  
    
    WITH unreadrequirements(coursecode) AS (
      SELECT requirescoursecode
      FROM requirements
      WHERE NEW.coursecode = requirements.coursecode
      EXCEPT
      SELECT coursecode
      FROM passedcourses
      WHERE NEW.studentid = passedcourses.studentid 
    )

    SELECT COUNT(coursecode) into nomeetrequirements FROM unreadrequirements;

    IF (nomeetrequirements > 0) THEN
      RAISE USING MESSAGE = 'Does not meet the requirements';
      RETURN NULL;
    END IF;
    
    SELECT COUNT(studentid) into amount FROM registrations
    WHERE NEW.coursecode = registrations.coursecode AND
              registrations.currentstatus = 'Registered';

    SELECT (CASE WHEN MAX(placeinqueue) IS NULL THEN 1 ELSE MAX(placeinqueue)+1 END) into positioninqueue FROM queuesfor
    WHERE NEW.coursecode = queuesfor.coursecode;
    
    SELECT capacity into coursecapacity FROM queueablecourse WHERE NEW.coursecode = queueablecourse.coursecode;
    
    IF (amount >= coursecapacity) THEN
      INSERT INTO queuesfor (coursecode, studentid, placeinqueue)
      VALUES (NEW.coursecode, NEW.studentid, positioninqueue);
      RETURN NULL;
    ELSE
      INSERT INTO takes (coursecode, studentid) 
      VALUES (NEW.coursecode, NEW.studentid);
      RETURN NULL;
    END IF;
  END;
$register_trigger$ LANGUAGE plpgsql;


CREATE TRIGGER register_trigger INSTEAD OF INSERT
ON registrations
	FOR EACH ROW EXECUTE PROCEDURE registration_function();

CREATE FUNCTION unregistration_function() RETURNS trigger AS $unregister_trigger$
	declare
	studentposition integer;
	coursecapacity integer;
	amount integer;
	studentschoolid TEXT;

	BEGIN
		SELECT placeinqueue into studentposition FROM queuesfor
		WHERE OLD.studentid = queuesfor.studentid AND
		      OLD.coursecode = queuesfor.coursecode;

		IF (OLD.currentstatus = 'Waiting') THEN
			DELETE FROM queuesfor 
			WHERE queuesfor.studentid = OLD.studentid AND
			      queuesfor.coursecode = OLD.coursecode;

			UPDATE queuesfor SET placeinqueue = placeinqueue - 1
			WHERE placeinqueue > studentposition AND
			      OLD.coursecode = queuesfor.coursecode;
			RETURN NULL;
		ELSE
			DELETE FROM takes 
			WHERE takes.studentid = OLD.studentid AND
			      takes.coursecode = OLD.coursecode;

			SELECT capacity into coursecapacity FROM queueablecourse 
			WHERE OLD.coursecode = queueablecourse.coursecode;

			SELECT COUNT(studentid) into amount FROM registrations
			WHERE OLD.coursecode = registrations.coursecode AND
				      registrations.currentstatus = 'Registered';

			IF(amount < coursecapacity) THEN
				SELECT studentid into studentschoolid FROM queuesfor
				WHERE OLD.coursecode = queuesfor.coursecode AND
				      queuesfor.placeinqueue = 1;
				DELETE FROM queuesfor
				WHERE queuesfor.studentid = studentschoolid AND
				      queuesfor.coursecode = OLD.coursecode;
				INSERT INTO takes (coursecode, studentid) 
				VALUES (OLD.coursecode, studentschoolid);
				
				UPDATE queuesfor SET placeinqueue = placeinqueue - 1
				WHERE OLD.coursecode = queuesfor.coursecode;
				RETURN NULL;
			END IF;
			RETURN NULL;
		END IF;
	END;
$unregister_trigger$ LANGUAGE plpgsql;

CREATE TRIGGER unregister_trigger INSTEAD OF DELETE
ON registrations
	FOR EACH ROW EXECUTE PROCEDURE unregistration_function();
