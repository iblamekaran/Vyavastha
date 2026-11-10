package dao; 

import database.DBConnection; 
import exception.DatabaseException; 
import model.Allocation; 
import model.Invigilator; 
import model.Room; 
import model.Seat; 
import model.Student; 

import java.sql.Connection; 
import java.sql.Date; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.time.LocalDate; 
import java.util.ArrayList; 
import java.util.HashMap; 
import java.util.LinkedHashMap; 
import java.util.List; 
import java.util.Map; 

public class AllocationDAO { 
    public void saveSession(LocalDate date, String slot, List<Allocation> allocations, 
                            Map<Room, Invigilator> invigilators) { 
        try (Connection c = DBConnection.getConnection()) { 
            c.setAutoCommit(false); 
            try { 
                clear(c, date, slot); 
                String seatSql = "INSERT INTO allocation " 
                    + "(exam_date, slot, roll_no, room_id, row_no, col_no) VALUES (?,?,?,?,?,?)"; 
                try (PreparedStatement ps = c.prepareStatement(seatSql)) { 
                    for (Allocation a : allocations) { 
                        ps.setDate(1, Date.valueOf(date)); 
                        ps.setString(2, slot); 
                        ps.setInt(3, a.getStudent().getRollNo()); 
                        ps.setInt(4, a.getSeat().getRoom().getRoomId()); 
                        ps.setInt(5, a.getSeat().getRow()); 
                        ps.setInt(6, a.getSeat().getCol()); 
                        ps.addBatch(); 
                    } 
                    ps.executeBatch(); 
                } 
                String invSql = "INSERT INTO invigilator_assignment " 
                    + "(exam_date, slot, room_id, invigilator_id) VALUES (?,?,?,?)"; 
                try (PreparedStatement ps = c.prepareStatement(invSql)) { 
                    for (Map.Entry<Room, Invigilator> e : invigilators.entrySet()) { 
                        ps.setDate(1, Date.valueOf(date)); 
                        ps.setString(2, slot); 
                        ps.setInt(3, e.getKey().getRoomId()); 
                        ps.setInt(4, e.getValue().getInvigilatorId()); 
                        ps.addBatch(); 
                    } 
                    ps.executeBatch(); 
                } 
                c.commit(); 
            } catch (SQLException e) { 
                c.rollback(); 
                throw e; 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not save allocation (nothing was changed): " 
                + e.getMessage(), e); 
        } 
    } 

    public void deleteSession(LocalDate date, String slot) { 
        try (Connection c = DBConnection.getConnection()) { 
            clear(c, date, slot); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not clear session: " + e.getMessage(), e); 
        } 
    } 

    private void clear(Connection c, LocalDate date, String slot) throws SQLException { 
        for (String table : new String[]{"invigilator_assignment", "allocation"}) { 
            try (PreparedStatement ps = c.prepareStatement( 
                    "DELETE FROM " + table + " WHERE exam_date = ? AND slot = ?")) { 
                ps.setDate(1, Date.valueOf(date)); 
                ps.setString(2, slot); 
                ps.executeUpdate(); 
            } 
        } 
    } 

    public List<Allocation> findBySession(LocalDate date, String slot) { 
        String sql = "SELECT a.roll_no, s.name, s.subject_code, " 
            + "r.room_id, r.room_name, r.total_rows, r.total_cols, a.row_no, a.col_no " 
            + "FROM allocation a " 
            + "JOIN student s ON a.roll_no = s.roll_no " 
            + "JOIN room r ON a.room_id = r.room_id " 
            + "WHERE a.exam_date = ? AND a.slot = ? " 
            + "ORDER BY r.room_id, a.row_no, a.col_no"; 
        List<Allocation> list = new ArrayList<>(); 
        Map<Integer, Room> rooms = new HashMap<>(); 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) { 
            ps.setDate(1, Date.valueOf(date)); 
            ps.setString(2, slot); 
            try (ResultSet rs = ps.executeQuery()) { 
                while (rs.next()) { 
                    int roomId = rs.getInt("room_id"); 
                    Room room = rooms.get(roomId); 
                    if (room == null) { 
                        room = new Room(roomId, rs.getString("room_name"), 
                            rs.getInt("total_rows"), rs.getInt("total_cols")); 
                        rooms.put(roomId, room); 
                    } 
                    Student student = new Student(rs.getInt("roll_no"), rs.getString("name"), 
                        rs.getString("subject_code")); 
                    Seat seat = new Seat(room, rs.getInt("row_no"), rs.getInt("col_no")); 
                    list.add(new Allocation(date, slot, student, seat)); 
                } 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not load allocation: " + e.getMessage(), e); 
        } 
        return list; 
    } 

    // room_id -> invigilator name, for one session 
    public Map<Integer, String> findInvigilators(LocalDate date, String slot) { 
        String sql = "SELECT ia.room_id, i.name FROM invigilator_assignment ia " 
            + "JOIN invigilator i ON ia.invigilator_id = i.invigilator_id " 
            + "WHERE ia.exam_date = ? AND ia.slot = ? ORDER BY ia.room_id"; 
        Map<Integer, String> map = new LinkedHashMap<>(); 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) { 
            ps.setDate(1, Date.valueOf(date)); 
            ps.setString(2, slot); 
            try (ResultSet rs = ps.executeQuery()) { 
                while (rs.next()) map.put(rs.getInt("room_id"), rs.getString("name")); 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not load invigilators: " + e.getMessage(), e); 
        } 
        return map; 
    } 
}
