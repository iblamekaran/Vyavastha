CREATE DATABASE IF NOT EXISTS vyavastha; 
USE vyavastha; 

CREATE TABLE student ( 
    roll_no INT PRIMARY KEY, 
    name VARCHAR(100) NOT NULL, 
    subject_code VARCHAR(20) NOT NULL 
); 

CREATE TABLE room ( 
    room_id INT AUTO_INCREMENT PRIMARY KEY, 
    room_name VARCHAR(50) NOT NULL UNIQUE, 
    total_rows INT NOT NULL, 
    total_cols INT NOT NULL 
); 

CREATE TABLE exam ( 
    exam_id INT AUTO_INCREMENT PRIMARY KEY, 
    subject_code VARCHAR(20) NOT NULL, 
    exam_date DATE NOT NULL, 
    slot VARCHAR(20) NOT NULL 
); 

CREATE TABLE invigilator ( 
    invigilator_id INT AUTO_INCREMENT PRIMARY KEY, 
    name VARCHAR(100) NOT NULL 
); 

CREATE TABLE allocation ( 
    allocation_id INT AUTO_INCREMENT PRIMARY KEY, 
    exam_date DATE NOT NULL, 
    slot VARCHAR(20) NOT NULL, 
    roll_no INT NOT NULL, 
    room_id INT NOT NULL, 
    row_no INT NOT NULL, 
    col_no INT NOT NULL, 
    UNIQUE (exam_date, slot, roll_no), 
    UNIQUE (exam_date, slot, room_id, row_no, col_no), 
    FOREIGN KEY (roll_no) REFERENCES student(roll_no) ON DELETE CASCADE, 
    FOREIGN KEY (room_id) REFERENCES room(room_id) ON DELETE CASCADE 
); 

CREATE TABLE invigilator_assignment ( 
    assignment_id INT AUTO_INCREMENT PRIMARY KEY, 
    exam_date DATE NOT NULL, 
    slot VARCHAR(20) NOT NULL, 
    room_id INT NOT NULL, 
    invigilator_id INT NOT NULL, 
    UNIQUE (exam_date, slot, room_id), 
    UNIQUE (exam_date, slot, invigilator_id), 
    FOREIGN KEY (room_id) REFERENCES room(room_id) ON DELETE CASCADE, 
    FOREIGN KEY (invigilator_id) REFERENCES invigilator(invigilator_id) ON DELETE CASCADE 
); 
