import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.*;

public class OSBox extends JFrame {

    private JTextArea editor;
    private JTextArea buildOutput;
    private File currentFile = null;
    private JLabel status;

    public OSBox() {
        super("OSBox IDE");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton saveBtn = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As");
        JButton compileBtn = new JButton("Compile");

        saveBtn.addActionListener(e -> save());
        saveAsBtn.addActionListener(e -> saveAs());
        compileBtn.addActionListener(e -> compile());

        toolbar.add(saveBtn);
        toolbar.add(saveAsBtn);
        toolbar.add(compileBtn);

        add(toolbar, BorderLayout.NORTH);

        // Editor
        editor = new JTextArea();
        editor.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setRowHeaderView(new LineNumberPanel(editor));

        // Build output
        buildOutput = new JTextArea();
        buildOutput.setEditable(false);
        buildOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane outputScroll = new JScrollPane(buildOutput);
        outputScroll.setPreferredSize(new Dimension(900, 150));
        outputScroll.setBorder(BorderFactory.createTitledBorder("Build Output"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScroll, outputScroll);
        split.setResizeWeight(0.8);
        add(split, BorderLayout.CENTER);

        // Status bar
        status = new JLabel("Ready");
        status.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(status, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void save() {
        if (currentFile == null) {
            saveAs();
            return;
        }
        try (FileWriter fw = new FileWriter(currentFile)) {
            fw.write(editor.getText());
            status.setText("Saved: " + currentFile.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
        }
    }

    private void saveAs() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fc.getSelectedFile();
            save();
        }
    }

    private void compile() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(this, "Save your kernel.c first!");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Make sure you have installed:\n" +
                        "gcc, ld, grub-mkrescue, xorriso, mtools, as\n\n" +
                        "Continue?",
                "Warning",
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) return;

        buildOutput.setText("");
        status.setText("Compiling...");

        try {
            // Folder where kernel.c is saved
            File projectDir = currentFile.getParentFile();

            // Build folder inside project
            File buildDir = new File(projectDir, "Build");
            if (!buildDir.exists() && !buildDir.mkdirs()) {
                throw new IOException("Failed to create Build directory");
            }

            // Write kernel.c
            writeFile(new File(buildDir, "kernel.c"), editor.getText());

            // Templates
            writeFile(new File(buildDir, "boot.s"),
                    "/* boot.s */\n" +
                    ".section .text\n" +
                    ".global _start\n" +
                    "_start:\n" +
                    "    call kmain\n" +
                    "hang:\n" +
                    "    jmp hang\n");

            writeFile(new File(buildDir, "linker.ld"),
                    "ENTRY(_start)\n" +
                    "SECTIONS\n" +
                    "{\n" +
                    "  . = 1M;\n" +
                    "  .text : { *(.text*) }\n" +
                    "  .rodata : { *(.rodata*) }\n" +
                    "  .data : { *(.data*) }\n" +
                    "  .bss : { *(.bss*) }\n" +
                    "}\n");

            writeFile(new File(buildDir, "grub.cfg"),
                    "set timeout=0\n" +
                    "set default=0\n" +
                    "menuentry \"OS\" {\n" +
                    "  multiboot /boot/kernel.bin\n" +
                    "  boot\n" +
                    "}\n");

            // Commands
            run("as --32 \"" + buildDir + "/boot.s\" -o \"" + buildDir + "/boot.o\"");
            run("gcc -m32 -ffreestanding -O2 -c \"" + buildDir + "/kernel.c\" -o \"" + buildDir + "/kernel.o\"");
            run("ld -m elf_i386 -T \"" + buildDir + "/linker.ld\" -nostdlib \"" + buildDir + "/boot.o\" \"" + buildDir + "/kernel.o\" -o \"" + buildDir + "/kernel.bin\"");

            run("mkdir -p \"" + buildDir + "/iso/boot/grub\"");
            run("cp \"" + buildDir + "/kernel.bin\" \"" + buildDir + "/iso/boot/\"");
            run("cp \"" + buildDir + "/grub.cfg\" \"" + buildDir + "/iso/boot/grub/\"");

            run("grub-mkrescue -o \"" + buildDir + "/output.iso\" \"" + buildDir + "/iso\"");

            status.setText("Build complete: " + buildDir + "/output.iso");
            JOptionPane.showMessageDialog(this, "Build complete!\nISO saved to:\n" + buildDir + "/output.iso");

        } catch (Exception ex) {
            status.setText("Build failed");
            appendOutput("ERROR: " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Compile error: " + ex.getMessage());
        }
    }

    private void writeFile(File f, String content) throws IOException {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(content);
        }
    }

    private void run(String cmd) throws Exception {
        appendOutput("$ " + cmd + "\n");
        Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});

        try (BufferedReader std = new BufferedReader(new InputStreamReader(p.getInputStream()));
             BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {

            String line;
            while ((line = std.readLine()) != null) appendOutput(line + "\n");
            while ((line = err.readLine()) != null) appendOutput(line + "\n");
        }

        int code = p.waitFor();
        if (code != 0) {
            throw new RuntimeException("Command failed with exit code " + code + ": " + cmd);
        }
    }

    private void appendOutput(String s) {
        buildOutput.append(s);
        buildOutput.setCaretPosition(buildOutput.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(OSBox::new);
    }
}

// VSCode-style line numbers
class LineNumberPanel extends JPanel implements DocumentListener {

    private final JTextArea textArea;

    public LineNumberPanel(JTextArea ta) {
        this.textArea = ta;
        ta.getDocument().addDocumentListener(this);
        setPreferredSize(new Dimension(50, Integer.MAX_VALUE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Rectangle clip = g.getClipBounds();
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();

        int startOffset = textArea.viewToModel(new Point(0, clip.y));
        int endOffset = textArea.viewToModel(new Point(0, clip.y + clip.height));

        int startLine = textArea.getDocument().getDefaultRootElement().getElementIndex(startOffset);
        int endLine = textArea.getDocument().getDefaultRootElement().getElementIndex(endOffset);

        for (int i = startLine; i <= endLine; i++) {
            try {
                int lineStartOffset = textArea.getLineStartOffset(i);
                Rectangle r = textArea.modelToView(lineStartOffset);
                int y = r.y + r.height - 4;

                String num = String.valueOf(i + 1);

                g.setColor(new Color(240, 240, 240));
                g.fillRect(0, r.y, getWidth(), r.height);

                g.setColor(Color.GRAY);
                g.drawString(num, getWidth() - fm.stringWidth(num) - 5, y);

                g.setColor(new Color(200, 200, 200));
                g.drawLine(getWidth() - 2, r.y, getWidth() - 2, r.y + r.height);
            } catch (Exception ignored) {}
        }
    }

    @Override public void insertUpdate(DocumentEvent e) { repaint(); }
    @Override public void removeUpdate(DocumentEvent e) { repaint(); }
    @Override public void changedUpdate(DocumentEvent e) { repaint(); }
}
