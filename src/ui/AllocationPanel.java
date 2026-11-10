package ui; 

import model.Invigilator; 
import model.Room; 
import service.AllocationService; 
import service.ExamService; 

import javax.swing.BorderFactory; 
import javax.swing.JButton; 
import javax.swing.JComboBox; 
import javax.swing.JLabel; 
import javax.swing.JPanel; 
import javax.swing.JScrollPane; 
import javax.swing.JTextArea; 
import javax.swing.JTextField; 
import java.awt.BorderLayout; 
import java.awt.FlowLayout; 
import java.awt.Font; 
import java.time.LocalDate; 
import java.util.Map; 

public class AllocationPanel extends JPanel { 
    private final AllocationService service = new AllocationService(); 
    private final JTextField dateField = new JTextField("2026-12-10", 10); 
    private final JComboBox<String> slotBox = 
        new JComboBox<>(ExamService.SLOTS.toArray(new String[0])); 
    private final JTextArea output = new JTextArea(); 

    public AllocationPanel() { 
        super(new BorderLayout(8, 8)); 
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        JButton runButton = new JButton("Run allocation"); 
        JButton clearButton = new JButton("Clear this session"); 
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        top.add(new JLabel("Date (yyyy-MM-dd):")); 
        top.add(dateField); 
        top.add(new JLabel("Slot:")); 
        top.add(slotBox); 
        top.add(runButton); 
        top.add(clearButton); 
        output.setEditable(false); 
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); 
        output.setText("Pick a date and slot, then press 'Run allocation'.\n" 
            + "Make sure exams, students, rooms and invigilators exist first."); 
        add(top, BorderLayout.NORTH); 
        add(new JScrollPane(output), BorderLayout.CENTER); 
        runButton.addActionListener(e -> runAllocation()); 
        clearButton.addActionListener(e -> clearSession()); 
    } 

    private void runAllocation() { 
        try { 
            LocalDate date = ExamService.parseDate(dateField.getText()); 
            String slot = (String) slotBox.getSelectedItem(); 
            AllocationService.Result r = service.allocateSession(date, slot); 
            StringBuilder sb = new StringBuilder(); 
            sb.append("Allocation saved for ").append(date).append(" ").append(slot).append("\n"); 
            sb.append("Students seated : ").append(r.allocations.size()).append("\n"); 
            sb.append("Rooms used      : ").append(r.invigilators.size()).append("\n\n"); 
            sb.append("Invigilators:\n"); 
            for (Map.Entry<Room, Invigilator> e : r.invigilators.entrySet()) { 
                sb.append("  ").append(e.getKey().getRoomName()).append(" -> ") 
                    .append(e.getValue().getName()).append("\n"); 
            } 
            sb.append("\nWarnings: ").append(r.warnings.isEmpty() ? "none" : "").append("\n"); 
            for (String w : r.warnings) sb.append("  - ").append(w).append("\n"); 
            sb.append("\nOpen the Reports tab to see the seating chart."); 
            output.setText(sb.toString()); 
            output.setCaretPosition(0); 
        } catch (RuntimeException ex) { 
            FormTablePanel.report(this, ex); 
        } 
    } 

    private void clearSession() { 
        try { 
            LocalDate date = ExamService.parseDate(dateField.getText()); 
            service.clearSession(date, (String) slotBox.getSelectedItem()); 
            output.setText("Saved allocation for this session was cleared."); 
        } catch (RuntimeException ex) { 
            FormTablePanel.report(this, ex); 
        } 
    } 
}
