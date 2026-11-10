package ui; 

import model.Exam; 
import service.ExamService; 

import javax.swing.JComboBox; 
import javax.swing.JTextField; 

public class ExamPanel extends FormTablePanel { 
    private final ExamService service = new ExamService(); 
    private final JTextField subjectField = new JTextField(10); 
    private final JTextField dateField = new JTextField("2026-12-10", 10); 
    private final JComboBox<String> slotBox = 
        new JComboBox<>(ExamService.SLOTS.toArray(new String[0])); 

    public ExamPanel() { 
        super(new String[]{"ID", "Subject", "Date", "Slot"}); 
        addField("Subject code:", subjectField); 
        addField("Date (yyyy-MM-dd):", dateField); 
        addField("Slot:", slotBox); 
        run(this::loadData); 
    } 

    @Override 
    protected void onAdd() { 
        service.add(subjectField.getText(), dateField.getText(), (String) slotBox.getSelectedItem()); 
        subjectField.setText(""); 
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
        for (Exam e : service.list()) { 
            model.addRow(new Object[]{e.getExamId(), e.getSubjectCode(), e.getExamDate(), e.getSlot()}); 
        } 
    } 
}
