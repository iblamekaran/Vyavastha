package service; 

import dao.AllocationDAO; 
import exception.ValidationException; 
import model.Allocation; 
import model.Room; 

import java.io.IOException; 
import java.nio.charset.StandardCharsets; 
import java.nio.file.Files; 
import java.nio.file.Path; 
import java.nio.file.Paths; 
import java.time.LocalDate; 
import java.util.ArrayList; 
import java.util.LinkedHashMap; 
import java.util.List; 
import java.util.Map; 

public class ReportService { 
    private final AllocationDAO dao = new AllocationDAO(); 

    public String seatingChart(LocalDate date, String slot) { 
        List<Allocation> list = load(date, slot); 
        Map<Integer, String> invigilators = dao.findInvigilators(date, slot); 
        Map<Room, List<Allocation>> byRoom = new LinkedHashMap<>(); 
        for (Allocation a : list) { 
            byRoom.computeIfAbsent(a.getSeat().getRoom(), k -> new ArrayList<>()).add(a); 
        } 
        StringBuilder sb = new StringBuilder(); 
        sb.append("SEATING CHART   ").append(date).append("   ").append(slot).append("\n"); 
        sb.append("Students seated: ").append(list.size()).append("\n\n"); 
        for (Map.Entry<Room, List<Allocation>> e : byRoom.entrySet()) { 
            Room room = e.getKey(); 
            String[][] cells = new String[room.getRows()][room.getCols()]; 
            for (Allocation a : e.getValue()) { 
                cells[a.getSeat().getRow() - 1][a.getSeat().getCol() - 1] = 
                    a.getStudent().getRollNo() + " (" + a.getStudent().getSubjectCode() + ")"; 
            } 
            sb.append(room.getRoomName()).append("  [").append(room.getRows()).append("x") 
                .append(room.getCols()).append("]   Invigilator: ") 
                .append(invigilators.getOrDefault(room.getRoomId(), "(none)")).append("\n"); 
            for (int r = 0; r < room.getRows(); r++) { 
                for (int c = 0; c < room.getCols(); c++) { 
                    sb.append(String.format("%-16s", cells[r][c] == null ? "-" : cells[r][c])); 
                } 
                sb.append("\n"); 
            } 
            sb.append("\n"); 
        } 
        return sb.toString(); 
    } 

    public Path exportCsv(LocalDate date, String slot) { 
        List<Allocation> list = load(date, slot); 
        Map<Integer, String> invigilators = dao.findInvigilators(date, slot); 
        List<String> lines = new ArrayList<>(); 
        lines.add("Date,Slot,Room,Row,Col,RollNo,Name,Subject,Invigilator"); 
        for (Allocation a : list) { 
            Room room = a.getSeat().getRoom(); 
            lines.add(String.join(",", 
                date.toString(), slot, csv(room.getRoomName()), 
                String.valueOf(a.getSeat().getRow()), String.valueOf(a.getSeat().getCol()), 
                String.valueOf(a.getStudent().getRollNo()), csv(a.getStudent().getName()), 
                csv(a.getStudent().getSubjectCode()), 
                csv(invigilators.getOrDefault(room.getRoomId(), "")))); 
        } 
        try { 
            Path dir = Paths.get("reports"); 
            Files.createDirectories(dir); 
            Path file = dir.resolve("seating_" + date + "_" + slot + ".csv"); 
            Files.write(file, lines, StandardCharsets.UTF_8); 
            return file; 
        } catch (IOException e) { 
            throw new IllegalStateException("Could not write CSV: " + e.getMessage(), e); 
        } 
    } 

    private List<Allocation> load(LocalDate date, String slot) { 
        List<Allocation> list = dao.findBySession(date, slot); 
        if (list.isEmpty()) { 
            throw new ValidationException("No allocation is saved for " + date + " (" + slot 
                + "). Run it from the Allocation tab first."); 
        } 
        return list; 
    } 

    // wrap in quotes so a comma inside a name cannot break the columns 
    private String csv(String text) { 
        return "\"" + text.replace("\"", "\"\"") + "\""; 
    } 
}
