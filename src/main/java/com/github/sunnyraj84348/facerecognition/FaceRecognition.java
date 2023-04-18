package com.github.sunnyraj84348.facerecognition;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Scanner;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
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

        createRes();
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        new FaceRecognition();
    }

    private void createRes() {
        var imgDir = new File("./images");
        var trainedDir = new File("./trained");
        var lblFile = new File("label_count");

        if (!imgDir.exists()) {
            imgDir.mkdir();
        }

        if (!trainedDir.exists()) {
            trainedDir.mkdir();
        }

        if (!lblFile.exists()) {
            try {
                var writer = new FileWriter("label_count");
                writer.write(Integer.toString(labelCount));
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
            setBtnEnabled(false);

            Thread worker = new Thread(() -> {
                // Capture image
                var img = webcam.getImage();

                try {
                    var file = new File("label_count");

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
                    setBtnEnabled(true);
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

                setBtnEnabled(true);
            });

            worker.start();
        });

        scanButton.addActionListener(e -> {
            setBtnEnabled(false);

            Thread worker = new Thread(() -> {
                // Capture image
                var bufImg = webcam.getImage();

                try {
                    // Save the captured image
                    ImageIO.write(bufImg, "JPG", new File("./images/temp.jpg"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                var testImage = getCroppedFace((JButton) e.getSource());
                if (testImage == null) {
                    setBtnEnabled(true);
                    return;
                }

                File root = new File("./trained");
                FilenameFilter imgFilter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        name = name.toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
                    }
                };

                File[] imageFiles = root.listFiles(imgFilter);

                if (imageFiles.length == 0) {
                    JOptionPane.showMessageDialog(this, "No image available for training");
                    return;
                }

                MatVector images = new MatVector(imageFiles.length);

                Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
                IntBuffer labelsBuf = labels.createBuffer();

                int counter = 0;

                for (File image : imageFiles) {
                    Mat img = imread(image.getAbsolutePath(), IMREAD_GRAYSCALE);
                    int label = Integer.parseInt(image.getName().split("\\-")[0]);

                    images.put(counter, img);
                    labelsBuf.put(counter, label);

                    counter++;
                }

                FaceRecognizer faceRecognizer = LBPHFaceRecognizer.create();
                faceRecognizer.train(images, labels);

                IntPointer label = new IntPointer(1);
                DoublePointer confidence = new DoublePointer(1);

                faceRecognizer.predict(testImage, label, confidence);
                faceRecognizer.close();

                int predictedLabel = label.get(0);

                if (confidence.get(0) >= 61) {
                    JOptionPane.showMessageDialog(this, "Not matched");
                    System.out.println("Confidence:" + confidence.get(0));
                } else {
                    JOptionPane.showMessageDialog(this, "Matched with label " + predictedLabel);
                    System.out.println("Confidence: " + confidence.get(0));
                }

                setBtnEnabled(true);
            });

            worker.start();
        });
    }

    private Mat getCroppedFace(JButton srcBtn) {
        Mat image;

        // Load the input image
        if (srcBtn.getText() == "Enroll") {
            image = imread("./images/" + (labelCount + 1) + "-image.jpg");
        } else {
            image = imread("./images/temp.jpg", IMREAD_GRAYSCALE);
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

    private void setBtnEnabled(boolean state) {
        connectButton.setEnabled(state);
        enrollButton.setEnabled(state);
        scanButton.setEnabled(state);
    }

    private void initUI() {
        this.setContentPane(panel);
        this.setSize(900, 750);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setVisible(true);
    }

    private void createUIComponents() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(this, "Failed loading webcam");
            System.exit(-1);
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcamPanel = new WebcamPanel(webcam, false);
    }
}
