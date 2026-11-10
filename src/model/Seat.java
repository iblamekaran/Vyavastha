package model; 

public class Seat { 
    private final Room room; 
    private final int row; 
    private final int col; 

    public Seat(Room room, int row, int col) { 
        this.room = room; 
        this.row = row; 
        this.col = col; 
    } 

    public Room getRoom() { return room; } 
    public int getRow() { return row; } 
    public int getCol() { return col; } 

    @Override 
    public String toString() { return room.getRoomName() + " R" + row + "C" + col; } 
}
