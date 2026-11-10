package dao; 

import database.DBConnection; 
import exception.DatabaseException; 
import model.Exam; 

import java.sql.Connection; 
import java.sql.Date; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.time.LocalDate; 
import java.util.ArrayList; 
import java.util.List; 

public class ExamDAO { 
    public void insert(Exam e) { 
        String sql = "INSERT INTO exam (subject_code, exam_date, slot) VALUES (?,?,?)"; 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) { 
            ps.setString(1, e.getSubjectCode()); 
            ps.setDate(2, Date.valueOf(e.getExamDate())); 
            ps.setString(3, e.getSlot()); 
            ps.executeUpdate(); 
        } catch (SQLException ex) { 
            throw new DatabaseException("Could not save exam: " + ex.getMessage(), ex); 
        } 
    } 

    public void delete(int examId) { 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement("DELETE FROM exam WHERE exam_id = ?")) { 
            ps.setInt(1, examId); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not delete exam: " + e.getMessage(), e); 
        } 
    } 

    public List<Exam> findAll() { 
        return query("SELECT exam_id, subject_code, exam_date, slot FROM exam " 
            + "ORDER BY exam_date, slot, subject_code", null, null); 
    } 

    public List<Exam> findBySession(LocalDate date, String slot) { 
        return query("SELECT exam_id, subject_code, exam_date, slot FROM exam " 
            + "WHERE exam_date = ? AND slot = ? ORDER BY subject_code", date, slot); 
    } 

    private List<Exam> query(String sql, LocalDate date, String slot) { 
        List<Exam> list = new ArrayList<>(); 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) { 
            if (date != null) { 
                ps.setDate(1, Date.valueOf(date)); 
                ps.setString(2, slot); 
            } 
            try (ResultSet rs = ps.executeQuery()) { 
                while (rs.next()) { 
                    list.add(new Exam(rs.getInt("exam_id"), rs.getString("subject_code"), 
                        rs.getDate("exam_date").toLocalDate(), rs.getString("slot"))); 
                } 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not load exams: " + e.getMessage(), e); 
        } 
        return list; 
    } 
}
