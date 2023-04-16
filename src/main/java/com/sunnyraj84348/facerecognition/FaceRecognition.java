package com.sunnyraj84348.facerecognition;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

public class FaceRecognition extends JFrame {
    private int labelCount = 0;
    private boolean isCamConnected = false;
    private WebcamPanel webcamPanel;
    private JPanel panel;
    private JButton enrollButton;
    private JButton scanButton;
    private JButton connectButton;
    private Webcam webcam;

    FaceRecognition() {
        initUI();
        initEvent();
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new FaceRecognition();
    }

    private void initEvent() {
        connectButton.addActionListener(e -> {
            connectButton.setEnabled(false);

            if (!isCamConnected) {
                webcamPanel.start();
                connectButton.setText("Disconnect");
                isCamConnected = true;

                enrollButton.setEnabled(true);
                scanButton.setEnabled(true);

            } else {
                webcamPanel.stop();
                connectButton.setText("Connect");
                isCamConnected = false;

                enrollButton.setEnabled(false);
                scanButton.setEnabled(false);
            }

            connectButton.setEnabled(true);
        });

        enrollButton.addActionListener(e -> {
            // Capture image
            var img = webcam.getImage();

            try {
                var file = new File("label_count");

                if (!file.exists()) {
                    var writer = new FileWriter("label_count");
                    writer.write(Integer.toString(labelCount));
                    writer.close();
                }

                var sc = new Scanner(file);
                labelCount = sc.nextInt();
                sc.close();

                // Save the captured image
                ImageIO.write(img, "JPG", new File("./images/" + (labelCount + 1) + "-image.jpg"));

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            var face = getCroppedFace((JButton) e.getSource());
            if (face == null) {
                return;
            }

            imwrite("./trained/" + (labelCount + 1) + "-image.jpg", face);
            labelCount++;

            try {
                var writer = new FileWriter("label_count");
                writer.write(Integer.toString(labelCount));
                writer.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            JOptionPane.showMessageDialog(this, "Completed");
        });
    }

    private Mat getCroppedFace(JButton srcBtn) {
        Mat image;

        // Load the input image
        if (srcBtn.getText() == "Enroll") {
            image = imread("./images/" + (labelCount + 1) + "-image.jpg");
        } else {
            image = imread("./images/" + (labelCount + 1) + "-image.jpg", IMREAD_GRAYSCALE);
        }

        // Create a face detector object
        CascadeClassifier faceDetector = new CascadeClassifier("./dataset/haarcascade_frontalface_default.xml");

        // Detect faces in the image
        RectVector faceDetections = new RectVector();
        faceDetector.detectMultiScale(image, faceDetections);

        if (faceDetections.size() == 0) {
            JOptionPane.showMessageDialog(this, "No face detected");
            return null;
        }

        if (faceDetections.size() > 1) {
            JOptionPane.showMessageDialog(this, "More than 1 face detected");
            return null;
        }

        // Crop detected face
        Rect rect = faceDetections.get(0);

        faceDetector.close();
        return new Mat(image, rect);
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
