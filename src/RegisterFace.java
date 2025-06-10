import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class RegisterFace {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final String FACE_CASCADE_PATH = "haarcascade_frontalface_alt.xml";
    private static final String FACE_DATA_PATH = "face_data/";
    private static volatile boolean captureRequested = false; // volatileでスレッド間同期

    public RegisterFace() {
    }

    public void start() {
        CascadeClassifier faceDetector = new CascadeClassifier(FACE_CASCADE_PATH);
        if (faceDetector.empty()) {
            System.err.println("カスケード分類器の読み込みに失敗しました");
            return;
        }
        System.out.println("RegisterFace: カスケード分類器の読み込み成功");

        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.err.println("カメラを開けませんでした");
            return;
        }
        System.out.println("RegisterFace: カメラが正常に開きました");

        JFrame frame = new JFrame("顔登録");
        JLabel label = new JLabel();
        frame.setLayout(new FlowLayout());
        frame.add(label);
        frame.setSize(640, 480);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    captureRequested = true;
                    System.out.println("RegisterFace: スペースキーが押されました");
                }
            }
        });

        new Thread(() -> {
            Mat frameMat = new Mat();
            while (camera.read(frameMat)) {
                System.out.println("RegisterFace: フレーム取得成功");

                Imgproc.cvtColor(frameMat, frameMat, Imgproc.COLOR_BGR2RGB);
                Mat gray = new Mat();
                Imgproc.cvtColor(frameMat, gray, Imgproc.COLOR_RGB2GRAY);

                MatOfRect faces = new MatOfRect();
                faceDetector.detectMultiScale(gray, faces);

                for (Rect rect : faces.toArray()) {
                    Imgproc.rectangle(frameMat, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height),
                            new Scalar(0, 255, 0), 2);

                    if (captureRequested) {
                        saveFace(gray.submat(rect));
                        captureRequested = false;
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(frame, "顔を登録しました！", "成功", JOptionPane.INFORMATION_MESSAGE);
                            camera.release();
                            frame.dispose();
                        });
                        return; // スレッド終了
                    }
                }

                BufferedImage image = convertMatToBufferedImage(frameMat);
                SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(image)));
                gray.release();
            }
            camera.release();
        }).start();
    }

    private static void saveFace(Mat face) {
        File dir = new File(FACE_DATA_PATH);
        if (!dir.exists()) dir.mkdir();

        Imgcodecs.imwrite(FACE_DATA_PATH + "user_face_1.jpg", face);

        Mat flippedFace = new Mat();
        Core.flip(face, flippedFace, 1);
        Imgcodecs.imwrite(FACE_DATA_PATH + "user_face_2.jpg", flippedFace);

        Mat brightFace = new Mat();
        face.convertTo(brightFace, -1, 1.2, 30);
        Imgcodecs.imwrite(FACE_DATA_PATH + "user_face_3.jpg", brightFace);

        Mat blurredFace = new Mat();
        Imgproc.GaussianBlur(face, blurredFace, new Size(3, 3), 0);
        Imgcodecs.imwrite(FACE_DATA_PATH + "user_face_4.jpg", blurredFace);

        System.out.println("顔画像の登録が完了しました！");
        SwingUtilities.invokeLater(() -> new FaceApp());
    }

    private static BufferedImage convertMatToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = new byte[width * height * (int) mat.elemSize()];
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, width, height, data);
        return image;
    }

    public static void main(String[] args) {
        RegisterFace registerFace = new RegisterFace();
        registerFace.start();
    }
}