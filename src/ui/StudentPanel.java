package ui; 

import exception.ValidationException; 
import model.Student; 
import service.StudentService; 

import javax.swing.JTextField; 

public class StudentPanel extends FormTablePanel { 
    private final StudentService service = new StudentService(); 
    private final JTextField rollField = new JTextField(10); 
    private final JTextField nameField = new JTextField(15); 
    private final JTextField subjectField = new JTextField(10); 

    public StudentPanel() { 
        super(new String[]{"Roll No", "Name", "Subject"}); 
        addField("Roll No:", rollField); 
        addField("Name:", nameField); 
        addField("Subject code:", subjectField); 
        run(this::loadData); 
    } 

    @Override 
    protected void onAdd() { 
        int roll; 
        try { 
            roll = Integer.parseInt(rollField.getText().trim()); 
        } catch (NumberFormatException e) { 
            throw new ValidationException("Roll number must be a number"); 
        } 
        service.add(roll, nameField.getText(), subjectField.getText()); 
        rollField.setText(""); 
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
        for (Student s : service.list()) { 
            model.addRow(new Object[]{s.getRollNo(), s.getName(), s.getSubjectCode()}); 
        } 
    } 
}
