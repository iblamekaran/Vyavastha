USE vyavastha; 

INSERT INTO student (roll_no, name, subject_code) VALUES 
(101,'Rahul','DBMS'),(102,'Aman','JAVA'),(103,'Priya','CN'), 
(104,'Simran','DBMS'),(105,'Arjun','JAVA'),(106,'Neha','CN'), 
(107,'Karan','DBMS'),(108,'Riya','JAVA'),(109,'Dev','CN'), 
(110,'Mehak','DBMS'),(111,'Yash','JAVA'),(112,'Tanya','CN'); 

INSERT INTO room (room_name, total_rows, total_cols) VALUES 
('Room A',2,2),('Room B',2,2),('Room C',2,2); 

INSERT INTO invigilator (name) VALUES ('Dr. Sharma'),('Prof. Gill'),('Dr. Kaur'),('Prof. Verma'); 

INSERT INTO exam (subject_code, exam_date, slot) VALUES 
('DBMS','2026-12-10','MORNING'),('JAVA','2026-12-10','MORNING'),('CN','2026-12-10','MORNING');
