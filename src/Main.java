import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select .vert file");
                chooser.setCurrentDirectory(new File("data"));
                chooser.setFileFilter(new FileNameExtensionFilter("VERT files (*.vert)", "vert"));

                int result = chooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) System.exit(0);

                File file = chooser.getSelectedFile();
                List<List<Vec2>> comps = VertLoader.load(file.getPath());

                JFrame frame = new JFrame("Discrete Curve Tool - " + file.getName());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(900, 700);
                frame.setLocationRelativeTo(null);

                frame.setContentPane(new CurvePanel(comps));
                frame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
