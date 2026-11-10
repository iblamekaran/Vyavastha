package service; 

import model.Allocation; 
import model.Room; 
import model.Seat; 

import java.util.ArrayList; 
import java.util.HashMap; 
import java.util.HashSet; 
import java.util.List; 
import java.util.Map; 
import java.util.Set; 

public class ConflictChecker { 
    // Hard errors: the allocation must be rejected 
    public List<String> findErrors(List<Allocation> list) { 
        List<String> errors = new ArrayList<>(); 
        Set<Integer> seenStudents = new HashSet<>(); 
        Set<String> seenSeats = new HashSet<>(); 
        for (Allocation a : list) { 
            Seat seat = a.getSeat(); 
            Room room = seat.getRoom(); 
            int roll = a.getStudent().getRollNo(); 
            if (!seenStudents.add(roll)) { 
                errors.add("Student " + roll + " is seated twice"); 
            } 
            if (!seenSeats.add(key(seat))) { 
                errors.add("Seat used twice: " + seat); 
            } 
            if (seat.getRow() < 1 || seat.getRow() > room.getRows() 
                || seat.getCol() < 1 || seat.getCol() > room.getCols()) { 
                errors.add("Seat outside room layout: " + seat); 
            } 
        } 
        return errors; 
    } 

    // Soft warnings: allowed, but shown to the admin 
    public List<String> findWarnings(List<Allocation> list) { 
        Map<String, Allocation> grid = new HashMap<>(); 
        for (Allocation a : list) grid.put(key(a.getSeat()), a); 
        List<String> warnings = new ArrayList<>(); 
        for (Allocation a : list) { 
            Seat s = a.getSeat(); 
            checkNeighbour(grid, a, s.getRoom().getRoomId(), s.getRow(), s.getCol() + 1, warnings); 
            checkNeighbour(grid, a, s.getRoom().getRoomId(), s.getRow() + 1, s.getCol(), warnings); 
        } 
        return warnings; 
    } 

    private void checkNeighbour(Map<String, Allocation> grid, Allocation a, 
                                int roomId, int row, int col, List<String> out) { 
        Allocation other = grid.get(roomId + ":" + row + ":" + col); 
        if (other != null && other.getStudent().getSubjectCode() 
            .equals(a.getStudent().getSubjectCode())) { 
            out.add("Same subject side by side: " + a.getStudent().getRollNo() 
                + " and " + other.getStudent().getRollNo() + " in " + a.getSeat().getRoom().getRoomName()); 
        } 
    } 

    private String key(Seat seat) { 
        return seat.getRoom().getRoomId() + ":" + seat.getRow() + ":" + seat.getCol(); 
    } 
}
