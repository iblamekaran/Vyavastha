package ui; 

import exception.ValidationException; 
import model.Room; 
import service.RoomService; 

import javax.swing.JTextField; 

public class RoomPanel extends FormTablePanel { 
    private final RoomService service = new RoomService(); 
    private final JTextField nameField = new JTextField(12); 
    private final JTextField rowsField = new JTextField(5); 
    private final JTextField colsField = new JTextField(5); 

    public RoomPanel() { 
        super(new String[]{"ID", "Room", "Rows", "Columns", "Capacity"}); 
        addField("Room name:", nameField); 
        addField("Rows:", rowsField); 
        addField("Columns:", colsField); 
        run(this::loadData); 
    } 

    @Override 
    protected void onAdd() { 
        int rows, cols; 
        try { 
            rows = Integer.parseInt(rowsField.getText().trim()); 
            cols = Integer.parseInt(colsField.getText().trim()); 
        } catch (NumberFormatException e) { 
            throw new ValidationException("Rows and columns must be numbers"); 
        } 
        service.add(nameField.getText(), rows, cols); 
        nameField.setText(""); 
        rowsField.setText(""); 
        colsField.setText(""); 
        loadData(); 
    } 

    @Override 
    protected void onDelete(int row) { 
        service.delete((Integer) model.getValueAt(row, 0)); 
        loadData(); 
    } 

    @Override 
    protected void loadData() { 
        model.setRowCount(0); 
        for (Room r : service.list()) { 
            model.addRow(new Object[]{r.getRoomId(), r.getRoomName(), r.getRows(), 
                r.getCols(), r.capacity()}); 
        } 
    } 
}
