package model; 

import java.util.Objects; 

public class Student { 
    private final int rollNo; 
    private String name; 
    private String subjectCode; 

    public Student(int rollNo, String name, String subjectCode) { 
        this.rollNo = rollNo; 
        this.name = name; 
        this.subjectCode = subjectCode; 
    } 

    public int getRollNo() { return rollNo; } 
    public String getName() { return name; } 
    public String getSubjectCode() { return subjectCode; } 
    public void setName(String name) { this.name = name; } 
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; } 

    @Override 
    public boolean equals(Object o) { 
        if (this == o) return true; 
        if (!(o instanceof Student)) return false; 
        return rollNo == ((Student) o).rollNo; 
    } 

    @Override 
    public int hashCode() { return Objects.hash(rollNo); } 

    @Override 
    public String toString() { return rollNo + " - " + name + " (" + subjectCode + ")"; } 
}
