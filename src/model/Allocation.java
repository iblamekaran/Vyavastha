package model; 

import java.time.LocalDate; 

public class Allocation { 
    private final LocalDate examDate; 
    private final String slot; 
    private final Student student; 
    private final Seat seat; 

    public Allocation(LocalDate examDate, String slot, Student student, Seat seat) { 
        this.examDate = examDate; 
        this.slot = slot; 
        this.student = student; 
        this.seat = seat; 
    } 

    public LocalDate getExamDate() { return examDate; } 
    public String getSlot() { return slot; } 
    public Student getStudent() { return student; } 
    public Seat getSeat() { return seat; } 
}
