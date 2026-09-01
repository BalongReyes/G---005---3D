package Core.Graphics;

import Core.Main;
import Settings.WindowSettings;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JFrame {

    private final Main main;
    public JLabel infoLabel;

    public ControlPanel(Main main) {
        this.main = main;
        initUI();
    }

    private void initUI() {
        setTitle("Control Panel");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(WindowSettings.backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info Label (FPS, Pos, Rot)
        infoLabel = new JLabel("FPS: 0");
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        mainPanel.add(infoLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // IsoLevel
        JLabel isoLabel = new JLabel("IsoLevel: " + (int)main.isoLevel);
        isoLabel.setForeground(Color.WHITE);
        JSlider isoSlider = new JSlider(-30, 30, (int)main.isoLevel);
        isoSlider.setBackground(WindowSettings.backgroundColor);
        isoSlider.setForeground(Color.WHITE);
        isoSlider.setFocusable(false);
        isoSlider.addChangeListener(e -> {
            isoLabel.setText("IsoLevel: " + isoSlider.getValue());
            if (!isoSlider.getValueIsAdjusting()) {
                main.updateTerrainMesh(isoSlider.getValue());
            }
        });
        mainPanel.add(isoLabel);
        mainPanel.add(isoSlider);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Noise Scale
        JLabel scaleLabel = new JLabel("Noise Scale: " + Settings.WorldSettings.NOISE_SCALE);
        scaleLabel.setForeground(Color.WHITE);
        JSlider scaleSlider = new JSlider(1, 20, (int)(Settings.WorldSettings.NOISE_SCALE * 1000));
        scaleSlider.setBackground(WindowSettings.backgroundColor);
        scaleSlider.setFocusable(false);
        scaleSlider.addChangeListener(e -> {
            scaleLabel.setText("Noise Scale: " + (scaleSlider.getValue() / 1000.0));
            if (!scaleSlider.getValueIsAdjusting()) {
                Settings.WorldSettings.NOISE_SCALE = scaleSlider.getValue() / 1000.0;
                main.regenerateTerrain();
            }
        });
        mainPanel.add(scaleLabel);
        mainPanel.add(scaleSlider);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Mountain Detail (Octaves)
        JLabel octaveLabel = new JLabel("Mountain Detail: " + Settings.WorldSettings.NOISE_OCTAVES);
        octaveLabel.setForeground(Color.WHITE);
        JSlider octaveSlider = new JSlider(1, 8, Settings.WorldSettings.NOISE_OCTAVES);
        octaveSlider.setBackground(WindowSettings.backgroundColor);
        octaveSlider.setFocusable(false);
        octaveSlider.addChangeListener(e -> {
            octaveLabel.setText("Mountain Detail: " + octaveSlider.getValue());
            if (!octaveSlider.getValueIsAdjusting()) {
                Settings.WorldSettings.NOISE_OCTAVES = octaveSlider.getValue();
                main.regenerateTerrain();
            }
        });
        mainPanel.add(octaveLabel);
        mainPanel.add(octaveSlider);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Water Level
        JLabel waterLabel = new JLabel("Water Level: " + (int)Settings.WorldSettings.WATER_LEVEL);
        waterLabel.setForeground(Color.WHITE);
        JSlider waterSlider = new JSlider(-50, 0, (int)Settings.WorldSettings.WATER_LEVEL);
        waterSlider.setBackground(WindowSettings.backgroundColor);
        waterSlider.setFocusable(false);
        waterSlider.addChangeListener(e -> {
            waterLabel.setText("Water Level: " + waterSlider.getValue());
            if (!waterSlider.getValueIsAdjusting()) {
                Settings.WorldSettings.WATER_LEVEL = waterSlider.getValue();
                main.updateTerrainMesh(main.isoLevel);
            }
        });
        mainPanel.add(waterLabel);
        mainPanel.add(waterSlider);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Camera Speed
        JLabel speedLabel = new JLabel("Camera Speed: " + Settings.WorldSettings.CAMERA_SPEED);
        speedLabel.setForeground(Color.WHITE);
        JSlider speedSlider = new JSlider(0, 1000, (int)(Settings.WorldSettings.CAMERA_SPEED * 100));
        speedSlider.setBackground(WindowSettings.backgroundColor);
        speedSlider.setFocusable(false);
        speedSlider.addChangeListener(e -> {
            speedLabel.setText("Camera Speed: " + (speedSlider.getValue() / 100.0));
            Settings.WorldSettings.CAMERA_SPEED = speedSlider.getValue() / 100.0;
        });
        mainPanel.add(speedLabel);
        mainPanel.add(speedSlider);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Wave Intensity
        JLabel waveLabel = new JLabel("Wave Intensity: " + Settings.WorldSettings.WAVE_INTENSITY);
        waveLabel.setForeground(Color.WHITE);
        JSlider waveSlider = new JSlider(0, 500, (int)(Settings.WorldSettings.WAVE_INTENSITY * 100));
        waveSlider.setBackground(WindowSettings.backgroundColor);
        waveSlider.setFocusable(false);
        waveSlider.addChangeListener(e -> {
            waveLabel.setText("Wave Intensity: " + (waveSlider.getValue() / 100.0));
            Settings.WorldSettings.WAVE_INTENSITY = waveSlider.getValue() / 100.0;
        });
        mainPanel.add(waveLabel);
        mainPanel.add(waveSlider);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Seed
        JLabel seedLabel = new JLabel("World Seed (Press Enter)");
        seedLabel.setForeground(Color.WHITE);
        JTextField seedField = new JTextField(String.valueOf(Settings.WorldSettings.SEED));
        seedField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        seedField.addActionListener(e -> {
            try {
                long newSeed = Long.parseLong(seedField.getText());
                Settings.WorldSettings.SEED = newSeed;
                main.regenerateTerrain();
            } catch (NumberFormatException ex) {
                seedField.setText(String.valueOf(Settings.WorldSettings.SEED));
            }
        });
        mainPanel.add(seedLabel);
        mainPanel.add(seedField);

        getContentPane().add(mainPanel);
        pack();
        setLocation(100, 100);
        setVisible(true);
    }
}
