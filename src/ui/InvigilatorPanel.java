package ui; 

import model.Invigilator; 
import service.InvigilatorService; 

import javax.swing.JTextField; 

public class InvigilatorPanel extends FormTablePanel { 
    private final InvigilatorService service = new InvigilatorService(); 
    private final JTextField nameField = new JTextField(15); 

    public InvigilatorPanel() { 
        super(new String[]{"ID", "Name"}); 
        addField("Name:", nameField); 
        run(this::loadData); 
    } 

    @Override 
    protected void onAdd() { 
        service.add(nameField.getText()); 
        nameField.setText(""); 
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
        for (Invigilator i : service.list()) { 
            model.addRow(new Object[]{i.getInvigilatorId(), i.getName()}); 
        } 
    } 
}
