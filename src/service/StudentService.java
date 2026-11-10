package service; 

import dao.StudentDAO; 
import exception.ValidationException; 
import model.Student; 

import java.util.List; 

public class StudentService { 
    private final StudentDAO dao = new StudentDAO(); 

    public void add(int rollNo, String name, String subject) { 
        if (rollNo <= 0) throw new ValidationException("Roll number must be a positive number"); 
        if (name == null || name.isBlank()) throw new ValidationException("Name cannot be empty"); 
        if (subject == null || subject.isBlank()) throw new ValidationException("Subject cannot be empty"); 
        if (dao.exists(rollNo)) throw new ValidationException("Roll number " + rollNo + " already exists"); 
        dao.insert(new Student(rollNo, name.trim(), subject.trim().toUpperCase())); 
    } 

    public void delete(int rollNo) { dao.delete(rollNo); } 
    public List<Student> list() { return dao.findAll(); } 
}
