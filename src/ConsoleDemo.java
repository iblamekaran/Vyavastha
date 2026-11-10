import allocation.SeatingAlgorithm; 
import model.Allocation; 
import model.Room; 
import model.Student; 
import service.ConflictChecker; 

import java.time.LocalDate; 
import java.util.ArrayList; 
import java.util.List; 

public class ConsoleDemo { 
    public static void main(String[] args) { 
        String[] names = {"Rahul", "Aman", "Priya", "Simran", "Arjun", "Neha", 
                          "Karan", "Riya", "Dev", "Mehak", "Yash", "Tanya"}; 
        String[] subjects = {"DBMS", "JAVA", "CN"}; 
        List<Student> students = new ArrayList<>(); 
        for (int i = 0; i < names.length; i++) { 
            students.add(new Student(101 + i, names[i], subjects[i % 3])); 
        } 
        List<Room> rooms = List.of(new Room(1, "Room A", 2, 2), 
                                   new Room(2, "Room B", 2, 2), 
                                   new Room(3, "Room C", 2, 2)); 
        List<Allocation> result = new SeatingAlgorithm() 
            .allocate(LocalDate.of(2026, 12, 10), "MORNING", students, rooms); 
        for (Allocation a : result) { 
            System.out.println(a.getSeat() + " -> " + a.getStudent()); 
        } 
        ConflictChecker checker = new ConflictChecker(); 
        System.out.println("Errors: " + checker.findErrors(result)); 
        System.out.println("Warnings: " + checker.findWarnings(result)); 
    } 
}
