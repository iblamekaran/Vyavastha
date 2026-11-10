package model; 

import java.util.Objects; 

public class Room { 
    private final int roomId; 
    private final String roomName; 
    private final int rows; 
    private final int cols; 

    public Room(int roomId, String roomName, int rows, int cols) { 
        this.roomId = roomId; 
        this.roomName = roomName; 
        this.rows = rows; 
        this.cols = cols; 
    } 

    public int getRoomId() { return roomId; } 
    public String getRoomName() { return roomName; } 
    public int getRows() { return rows; } 
    public int getCols() { return cols; } 
    public int capacity() { return rows * cols; } 

    @Override 
    public boolean equals(Object o) { 
        if (this == o) return true; 
        if (!(o instanceof Room)) return false; 
        return roomId == ((Room) o).roomId; 
    } 

    @Override 
    public int hashCode() { return Objects.hash(roomId); } 

    @Override 
    public String toString() { return roomName + " (" + rows + "x" + cols + ")"; } 
}
