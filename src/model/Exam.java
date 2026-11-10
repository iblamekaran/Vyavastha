package model; 

import java.time.LocalDate; 

public class Exam { 
    private final int examId; 
    private final String subjectCode; 
    private final LocalDate examDate; 
    private final String slot; 

    public Exam(int examId, String subjectCode, LocalDate examDate, String slot) { 
        this.examId = examId; 
        this.subjectCode = subjectCode; 
        this.examDate = examDate; 
        this.slot = slot; 
    } 

    public int getExamId() { return examId; } 
    public String getSubjectCode() { return subjectCode; } 
    public LocalDate getExamDate() { return examDate; } 
    public String getSlot() { return slot; } 
}
