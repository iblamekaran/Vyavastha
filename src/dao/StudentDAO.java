package dao; 

import database.DBConnection; 
import exception.DatabaseException; 
import model.Student; 

import java.sql.Connection; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.util.ArrayList; 
import java.util.Collection; 
import java.util.Collections; 
import java.util.List; 

public class StudentDAO { 
    public void insert(Student s) { 
        String sql = "INSERT INTO student (roll_no, name, subject_code) VALUES (?,?,?)"; 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) { 
            ps.setInt(1, s.getRollNo()); 
            ps.setString(2, s.getName()); 
            ps.setString(3, s.getSubjectCode()); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not save student: " + e.getMessage(), e); 
        } 
    } 

    public void delete(int rollNo) { 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement("DELETE FROM student WHERE roll_no = ?")) { 
            ps.setInt(1, rollNo); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not delete student: " + e.getMessage(), e); 
        } 
    } 

    public boolean exists(int rollNo) { 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM student WHERE roll_no = ?")) { 
            ps.setInt(1, rollNo); 
            try (ResultSet rs = ps.executeQuery()) { 
                return rs.next(); 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not check student: " + e.getMessage(), e); 
        } 
    } 

    public List<Student> findAll() { 
        return query("SELECT roll_no, name, subject_code FROM student ORDER BY roll_no", Collections.emptyList()); 
    } 

    public List<Student> findBySubjects(Collection<String> subjects) { 
        if (subjects.isEmpty()) return new ArrayList<>(); 
        String marks = String.join(",", Collections.nCopies(subjects.size(), "?")); 
        String sql = "SELECT roll_no, name, subject_code FROM student WHERE subject_code IN (" 
            + marks + ") ORDER BY roll_no"; 
        return query(sql, new ArrayList<>(subjects)); 
    } 

    private List<Student> query(String sql, List<String> params) { 
        List<Student> list = new ArrayList<>(); 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) { 
            for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i)); 
            try (ResultSet rs = ps.executeQuery()) { 
                while (rs.next()) { 
                    list.add(new Student(rs.getInt("roll_no"), rs.getString("name"), 
                        rs.getString("subject_code"))); 
                } 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not load students: " + e.getMessage(), e); 
        } 
        return list; 
    } 
}
