package service; 

import dao.ExamDAO; 
import exception.ValidationException; 
import model.Exam; 

import java.time.LocalDate; 
import java.time.format.DateTimeParseException; 
import java.util.List; 

public class ExamService { 
    public static final List<String> SLOTS = List.of("MORNING", "AFTERNOON", "EVENING"); 
    private final ExamDAO dao = new ExamDAO(); 

    public static LocalDate parseDate(String text) { 
        try { 
            return LocalDate.parse(text == null ? "" : text.trim()); 
        } catch (DateTimeParseException e) { 
            throw new ValidationException("Date must look like 2026-12-10 (yyyy-MM-dd)"); 
        } 
    } 

    public static String checkSlot(String slot) { 
        if (slot == null || !SLOTS.contains(slot)) { 
            throw new ValidationException("Slot must be one of " + SLOTS); 
        } 
        return slot; 
    } 

    public void add(String subject, String dateText, String slot) { 
        if (subject == null || subject.isBlank()) throw new ValidationException("Subject cannot be empty"); 
        LocalDate date = parseDate(dateText); 
        checkSlot(slot); 
        dao.insert(new Exam(0, subject.trim().toUpperCase(), date, slot)); 
    } 

    public void delete(int examId) { dao.delete(examId); } 
    public List<Exam> list() { return dao.findAll(); } 
}
