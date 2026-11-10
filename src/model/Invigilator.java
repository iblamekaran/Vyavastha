package model; 

public class Invigilator { 
    private final int invigilatorId; 
    private final String name; 

    public Invigilator(int invigilatorId, String name) { 
        this.invigilatorId = invigilatorId; 
        this.name = name; 
    } 

    public int getInvigilatorId() { return invigilatorId; } 
    public String getName() { return name; } 

    @Override 
    public String toString() { return name; } 
}
