package ui; 

import service.ExamService; 
import service.ReportService; 

import javax.swing.BorderFactory; 
import javax.swing.JButton; 
import javax.swing.JComboBox; 
import javax.swing.JLabel; 
import javax.swing.JOptionPane; 
import javax.swing.JPanel; 
import javax.swing.JScrollPane; 
import javax.swing.JTextArea; 
import javax.swing.JTextField; 
import java.awt.BorderLayout; 
import java.awt.FlowLayout; 
import java.awt.Font; 
import java.nio.file.Path; 
import java.time.LocalDate; 

public class ReportPanel extends JPanel { 
    private final ReportService service = new ReportService(); 
    private final JTextField dateField = new JTextField("2026-12-10", 10); 
    private final JComboBox<String> slotBox = 
        new JComboBox<>(ExamService.SLOTS.toArray(new String[0])); 
    private final JTextArea chart = new JTextArea(); 

    public ReportPanel() { 
        super(new BorderLayout(8, 8)); 
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        JButton showButton = new JButton("Show seating chart"); 
        JButton csvButton = new JButton("Export CSV"); 
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        top.add(new JLabel("Date (yyyy-MM-dd):")); 
        top.add(dateField); 
        top.add(new JLabel("Slot:")); 
        top.add(slotBox); 
        top.add(showButton); 
        top.add(csvButton); 
        chart.setEditable(false); 
        chart.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); 
        add(top, BorderLayout.NORTH); 
        add(new JScrollPane(chart), BorderLayout.CENTER); 

        showButton.addActionListener(e -> { 
            try { 
                LocalDate date = ExamService.parseDate(dateField.getText()); 
                chart.setText(service.seatingChart(date, (String) slotBox.getSelectedItem())); 
                chart.setCaretPosition(0); 
            } catch (RuntimeException ex) { 
                FormTablePanel.report(this, ex); 
            } 
        }); 

        csvButton.addActionListener(e -> { 
            try { 
                LocalDate date = ExamService.parseDate(dateField.getText()); 
                Path file = service.exportCsv(date, (String) slotBox.getSelectedItem()); 
                JOptionPane.showMessageDialog(this, "Saved to " + file.toAbsolutePath()); 
            } catch (RuntimeException ex) { 
                FormTablePanel.report(this, ex); 
            } 
        }); 
    } 
}
