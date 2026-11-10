package dao; 

import database.DBConnection; 
import exception.DatabaseException; 
import model.Room; 

import java.sql.Connection; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.util.ArrayList; 
import java.util.List; 

public class RoomDAO { 
    public void insert(Room r) { 
        String sql = "INSERT INTO room (room_name, total_rows, total_cols) VALUES (?,?,?)"; 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) { 
            ps.setString(1, r.getRoomName()); 
            ps.setInt(2, r.getRows()); 
            ps.setInt(3, r.getCols()); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not save room: " + e.getMessage(), e); 
        } 
    } 

    public void delete(int roomId) { 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement("DELETE FROM room WHERE room_id = ?")) { 
            ps.setInt(1, roomId); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not delete room: " + e.getMessage(), e); 
        } 
    } 

    public List<Room> findAll() { 
        List<Room> list = new ArrayList<>(); 
        String sql = "SELECT room_id, room_name, total_rows, total_cols FROM room ORDER BY room_id"; 
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) { 
            while (rs.next()) { 
                list.add(new Room(rs.getInt("room_id"), rs.getString("room_name"), 
                    rs.getInt("total_rows"), rs.getInt("total_cols"))); 
            } 
        } catch (SQLException e) { 
            throw new DatabaseException("Could not load rooms: " + e.getMessage(), e); 
        } 
        return list; 
    } 
}
