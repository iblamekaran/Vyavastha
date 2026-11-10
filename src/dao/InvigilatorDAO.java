package dao; 

import database.DBConnection; 
import exception.DatabaseException; 
import model.Invigilator; 

import java.sql.Connection; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.util.ArrayList; 
import java.util.List; 

public class InvigilatorDAO { 
    public void insert(Invigilator i) { 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement("INSERT INTO invigilator (name) VALUES (?)")) { 
            ps.setString(1, i.getName()); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not save invigilator: " + e.getMessage(), e); 
        } 
    } 

    public void delete(int id) { 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement("DELETE FROM invigilator WHERE invigilator_id = ?")) { 
            ps.setInt(1, id); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not delete invigilator: " + e.getMessage(), e); 
        } 
    } 

    public List<Invigilator> findAll() { 
        List<Invigilator> list = new ArrayList<>(); 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement( 
                 "SELECT invigilator_id, name FROM invigilator ORDER BY invigilator_id"); 
             ResultSet rs = ps.executeQuery()) { 
            while (rs.next()) { 
                list.add(new Invigilator(rs.getInt("invigilator_id"), rs.getString("name"))); 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not load invigilators: " + e.getMessage(), e); 
        } 
        return list; 
    } 
}
