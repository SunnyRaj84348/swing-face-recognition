package com.sunnyraj84348.facerecognition;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import javax.swing.*;

public class FaceRecognition extends JFrame {
    private WebcamPanel webcamPanel;
    private JPanel panel;
    private JButton enrollButton;
    private JButton scanButton;
    private JButton connectButton;

    private Webcam webcam;

    FaceRecognition() {
        initUI();
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new FaceRecognition();
    }

    private void initUI() {
        this.setContentPane(panel);
        this.setSize(900, 750);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void createUIComponents() {
        webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        webcamPanel = new WebcamPanel(webcam, false);
    }
}
