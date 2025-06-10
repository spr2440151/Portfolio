import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;


public class CameraHandler {
    private VideoCapture camera; // OpenCVのカメラオブジェクト
    private boolean isRunning; // カメラが動作中かの判定

    // コンストラクタ: カメラを初期化
    public CameraHandler() {
        camera = new VideoCapture(0); // デフォルトカメラ（インデックス0）
        isRunning = camera.isOpened();
        if (isRunning) {
            System.out.println("カメラが正常に起動しました。");
        } else {
            System.err.println("カメラの起動に失敗しました。");
        }
    }

    // captureImage: カメラから1フレームを取得しBufferedImageに変換
    public BufferedImage captureImage() {
        if (!isRunning) {
            System.err.println("カメラが利用できない状態です。");
            return null;
        }
        
        Mat frame = new Mat();
        boolean success = camera.read(frame);
        if (success && !frame.empty()) {
            System.out.println("フレームを正常に取得しました: " + frame.width() + "x" + frame.height());
            return matToBufferedImage(frame);
        } else {
            System.err.println("フレームの取得に失敗しました。");
            return null;
        }
    }

    // matToBufferedImage: OpenCVのMatをBufferedImageに変換
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        byte[] b = new byte[mat.cols() * mat.rows() * mat.channels()];
        mat.get(0, 0, b);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    // release: カメラリソースを解放
    public void release() {
        if (isRunning && camera != null) {
            camera.release();
            isRunning = false;
            System.out.println("カメラを解放しました。");
        }
    }

    // isRunning: カメラの動作状態を返す
    public boolean isRunning() {
        return isRunning;
    }
}