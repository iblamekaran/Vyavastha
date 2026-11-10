package service; 

import dao.RoomDAO; 
import exception.ValidationException; 
import model.Room; 

import java.util.List; 

public class RoomService { 
    private final RoomDAO dao = new RoomDAO(); 

    public void add(String name, int rows, int cols) { 
        if (name == null || name.isBlank()) throw new ValidationException("Room name cannot be empty"); 
        if (rows < 1 || rows > 30 || cols < 1 || cols > 30) { 
            throw new ValidationException("Rows and columns must be between 1 and 30"); 
        } 
        for (Room r : dao.findAll()) { 
            if (r.getRoomName().equalsIgnoreCase(name.trim())) { 
                throw new ValidationException("Room " + name.trim() + " already exists"); 
            } 
        } 
        dao.insert(new Room(0, name.trim(), rows, cols)); 
    } 

    public void delete(int roomId) { dao.delete(roomId); } 
    public List<Room> list() { return dao.findAll(); } 
}
