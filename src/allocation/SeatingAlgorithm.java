package allocation; 

import exception.RoomFullException; 
import model.Allocation; 
import model.Room; 
import model.Seat; 
import model.Student; 

import java.time.LocalDate; 
import java.util.ArrayDeque; 
import java.util.ArrayList; 
import java.util.Comparator; 
import java.util.Deque; 
import java.util.List; 
import java.util.Map; 
import java.util.TreeMap; 

public class SeatingAlgorithm { 
    public List<Allocation> allocate(LocalDate date, String slot, 
                                     List<Student> students, List<Room> rooms) { 
        int totalSeats = 0; 
        for (Room room : rooms) totalSeats += room.capacity(); 
        if (students.size() > totalSeats) { 
            throw new RoomFullException(students.size() + " students but only " 
                + totalSeats + " seats available"); 
        } 
        List<Student> sorted = new ArrayList<>(students); 
        sorted.sort(Comparator.comparingInt(Student::getRollNo)); 
        Map<String, Deque<Student>> bySubject = new TreeMap<>(); 
        for (Student s : sorted) { 
            bySubject.computeIfAbsent(s.getSubjectCode(), k -> new ArrayDeque<>()).add(s); 
        } 
        List<Allocation> result = new ArrayList<>(); 
        int remaining = sorted.size(); 
        for (Room room : rooms) { 
            if (remaining == 0) break; 
            String[][] grid = new String[room.getRows()][room.getCols()]; 
            for (int r = 0; r < room.getRows() && remaining > 0; r++) { 
                for (int c = 0; c < room.getCols() && remaining > 0; c++) { 
                    String left = (c > 0) ? grid[r][c - 1] : null; 
                    String up = (r > 0) ? grid[r - 1][c] : null; 
                    String chosen = pickSubject(bySubject, left, up); 
                    Deque<Student> group = bySubject.get(chosen); 
                    Student student = group.poll(); 
                    if (group.isEmpty()) bySubject.remove(chosen); 
                    grid[r][c] = chosen; 
                    result.add(new Allocation(date, slot, student, new Seat(room, r + 1, c + 1))); 
                    remaining--; 
                } 
            } 
        } 
        return result; 
    } 

    private String pickSubject(Map<String, Deque<Student>> bySubject, String left, String up) { 
        String best = null;      // biggest group that does not clash with a neighbour 
        String fallback = null;  // biggest group overall 
        for (Map.Entry<String, Deque<Student>> e : bySubject.entrySet()) { 
            String subject = e.getKey(); 
            int size = e.getValue().size(); 
            if (fallback == null || size > bySubject.get(fallback).size()) fallback = subject; 
            if (subject.equals(left) || subject.equals(up)) continue; 
            if (best == null || size > bySubject.get(best).size()) best = subject; 
        } 
        return (best != null) ? best : fallback; 
    } 
}
