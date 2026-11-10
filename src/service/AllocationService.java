package service; 

import allocation.SeatingAlgorithm; 
import dao.AllocationDAO; 
import dao.ExamDAO; 
import dao.InvigilatorDAO; 
import dao.RoomDAO; 
import dao.StudentDAO; 
import exception.AllocationException; 
import exception.ValidationException; 
import model.Allocation; 
import model.Exam; 
import model.Invigilator; 
import model.Room; 
import model.Student; 

import java.time.LocalDate; 
import java.util.LinkedHashMap; 
import java.util.LinkedHashSet; 
import java.util.List; 
import java.util.Map; 
import java.util.Set; 
import java.util.TreeSet; 

public class AllocationService { 
    public static class Result { 
        public final List<Allocation> allocations; 
        public final List<String> warnings; 
        public final Map<Room, Invigilator> invigilators; 

        public Result(List<Allocation> allocations, List<String> warnings, 
                      Map<Room, Invigilator> invigilators) { 
            this.allocations = allocations; 
            this.warnings = warnings; 
            this.invigilators = invigilators; 
        } 
    } 

    private final ExamDAO examDAO = new ExamDAO(); 
    private final StudentDAO studentDAO = new StudentDAO(); 
    private final RoomDAO roomDAO = new RoomDAO(); 
    private final InvigilatorDAO invigilatorDAO = new InvigilatorDAO(); 
    private final AllocationDAO allocationDAO = new AllocationDAO(); 
    private final SeatingAlgorithm algorithm = new SeatingAlgorithm(); 
    private final ConflictChecker checker = new ConflictChecker(); 

    public Result allocateSession(LocalDate date, String slot) { 
        ExamService.checkSlot(slot); 
        // 1. exams in this session -> subjects 
        List<Exam> exams = examDAO.findBySession(date, slot); 
        if (exams.isEmpty()) { 
            throw new ValidationException("No exams are scheduled on " + date + " (" + slot + ")"); 
        } 
        Set<String> subjects = new TreeSet<>(); 
        for (Exam e : exams) subjects.add(e.getSubjectCode()); 
        // 2. students of those subjects, and all rooms 
        List<Student> students = studentDAO.findBySubjects(subjects); 
        if (students.isEmpty()) { 
            throw new ValidationException("No students are registered for " + subjects); 
        } 
        List<Room> rooms = roomDAO.findAll(); 
        if (rooms.isEmpty()) throw new ValidationException("Add at least one room first"); 
        // 3. seating (throws RoomFullException when seats are not enough) 
        List<Allocation> allocations = algorithm.allocate(date, slot, students, rooms); 
        // 4. errors cancel everything, warnings are only reported 
        List<String> errors = checker.findErrors(allocations); 
        if (!errors.isEmpty()) { 
            throw new AllocationException("Allocation rejected: " + errors); 
        } 
        List<String> warnings = checker.findWarnings(allocations); 
        // 5. one invigilator per room that is actually used 
        Set<Room> usedRooms = new LinkedHashSet<>(); 
        for (Allocation a : allocations) usedRooms.add(a.getSeat().getRoom()); 
        List<Invigilator> pool = invigilatorDAO.findAll(); 
        if (pool.size() < usedRooms.size()) { 
            throw new AllocationException(usedRooms.size() + " rooms are in use but only " 
                + pool.size() + " invigilators exist. Add more invigilators."); 
        } 
        Map<Room, Invigilator> assignment = new LinkedHashMap<>(); 
        int next = 0; 
        for (Room room : usedRooms) assignment.put(room, pool.get(next++)); 
        // 6. save everything in one transaction 
        allocationDAO.saveSession(date, slot, allocations, assignment); 
        return new Result(allocations, warnings, assignment); 
    } 

    public void clearSession(LocalDate date, String slot) { 
        ExamService.checkSlot(slot); 
        allocationDAO.deleteSession(date, slot); 
    } 
}
