package service; 

import dao.InvigilatorDAO; 
import exception.ValidationException; 
import model.Invigilator; 

import java.util.List; 

public class InvigilatorService { 
    private final InvigilatorDAO dao = new InvigilatorDAO(); 

    public void add(String name) { 
        if (name == null || name.isBlank()) throw new ValidationException("Name cannot be empty"); 
        dao.insert(new Invigilator(0, name.trim())); 
    } 

    public void delete(int id) { dao.delete(id); } 
    public List<Invigilator> list() { return dao.findAll(); } 
}
