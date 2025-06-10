// OpenCV（画像処理ライブラリ）のコア機能を提供するクラス
import org.opencv.core.*;
 
// 画像の読み込み・保存を行うためのクラス
import org.opencv.imgcodecs.Imgcodecs;
 
// 画像の前処理（フィルタリングやヒストグラム均等化など）を行うためのクラス
import org.opencv.imgproc.Imgproc;
 
// Haar Cascade や LBP Cascade を利用した顔検出を行うためのクラス
import org.opencv.objdetect.CascadeClassifier;
 
// カメラの映像を取得するためのクラス（Webカメラなどのデバイス操作）
import org.opencv.videoio.VideoCapture;
 
// 画像上の点（座標）を表現するクラス（矩形の描画などに使用）
import org.opencv.core.Point;
 
 
// ==========================
// Java標準ライブラリ
// ==========================
 
// JavaのサウンドAPIを使用して音声を再生するためのクラス
import javax.sound.sampled.*;
 
// Swing（JavaのGUIライブラリ）の主要なクラス（フレームやボタンなどを作成）
import javax.swing.*;
 
// Javaのグラフィックス描画をサポートするクラス（カメラ映像の描画などに使用）
import java.awt.*;
 
// BufferedImage（画像データの処理）を扱うためのクラス
import java.awt.image.BufferedImage;
 
// ファイルの読み書きを行うためのクラス（画像データの保存や音声ファイルの読み込み）
import java.io.File;
 
// 音声ファイルの読み込みやエラーハンドリングを行うための例外処理
import java.io.IOException;
 
// データの検証やNullチェックなどに利用するユーティリティクラス
import java.util.Objects;
 
 
public class FaceLogin extends JPanel {
 
    // **OpenCVライブラリのロード**
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
 
    // **顔検出に使用するカスケード分類器のパス**
    private static final String FACE_CASCADE_PATH = "haarcascade_frontalface_alt.xml";
 
    // **登録済みの顔データを格納するディレクトリ**
    private static final String FACE_DATA_PATH = "face_data/";
 
    // **ログイン成功時に再生する音声ファイルのパス**
    private static final String SOUND_FILE_PATH = "/resources/login_success.wav";
 
    // **カメラの映像を描画するためのバッファ**
    private BufferedImage currentFrame;
 
    // **画面の横幅と縦幅**
    private int screenWidth, screenHeight;
 
    // **コンストラクタ（画面サイズを受け取る）**
    public FaceLogin(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    // **顔認証処理を開始するメソッド**
    public void start() {
        System.out.println("FaceLogin: プログラム開始");
 
        // **カスケード分類器（顔検出用）を読み込む**
        CascadeClassifier faceDetector = new CascadeClassifier(FACE_CASCADE_PATH);
        if (faceDetector.empty()) {
            System.err.println("FaceLogin: カスケード分類器の読み込みに失敗しました");
            return;
        }
        System.out.println("FaceLogin: カスケード分類器の読み込み成功");
 
        // **カメラを開く**
        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.err.println("FaceLogin: カメラを開けませんでした");
            return;
        }
        System.out.println("FaceLogin: カメラが正常に開きました");
 
        // **全画面表示のウィンドウ（JFrame）を作成**
        JFrame frame = new JFrame("Face Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // **全画面モード**
        frame.setUndecorated(true); // **タイトルバーを非表示**
        frame.setContentPane(this);
        frame.setVisible(true);
 
        Mat frameMat = new Mat(); // **カメラフレームを格納するMatオブジェクト**
        int faceNotFoundCount = 0; // **検出失敗のカウント**
        final int MAX_ATTEMPTS = 100; // **最大試行回数（失敗時に終了する）**
 
        // **カメラの映像を取得しながら処理を行うループ**
        while (camera.read(frameMat)) {
            System.out.println("FaceLogin: フレーム取得成功");
 
            // **画像をグレースケールに変換（顔検出の精度向上）**
            Imgproc.cvtColor(frameMat, frameMat, Imgproc.COLOR_BGR2RGB);
            Mat gray = new Mat();
            Imgproc.cvtColor(frameMat, gray, Imgproc.COLOR_RGB2GRAY);
 
            // **コントラスト補正 & ノイズ除去**
            Imgproc.equalizeHist(gray, gray);
            Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 0);
 
            // **顔を検出**
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(gray, faces);
 
            // **顔が見つからない場合の処理**
            if (faces.toArray().length == 0) {
                faceNotFoundCount++;
                System.out.println("FaceLogin: 顔が検出されませんでした");
            } else {
                faceNotFoundCount = 0;
                System.out.println("FaceLogin: 顔を検出！");
            }
 
            // **検出された顔の処理**
            for (Rect rect : faces.toArray()) {
                // **検出した顔の周囲に緑の四角を描画**
                Imgproc.rectangle(frameMat, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0), 3);
 
                // **顔認証（登録済みデータと比較）**
                if (isFaceMatched(gray.submat(rect))) {
                    System.out.println("FaceLogin: 顔認証成功！");
 
                    // **音声を再生**
                    playSound(SOUND_FILE_PATH);
 
                    // **ログイン成功ダイアログを表示**
                    JOptionPane.showMessageDialog(frame, "ログイン成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
 
                    // **カメラを停止し、ウィンドウを閉じる**
                    camera.release();
                    frame.dispose();
 
                    System.out.println("FaceLogin: 画像認識アプリを起動します...");
                   
                    // **画像認識アプリを起動**
                    SwingUtilities.invokeLater(() -> new ImageRecognitionApp());
                    return;
                } else {
                    System.out.println("FaceLogin: 顔認証に失敗しました");
                }
            }
 
            // **カメラの映像を描画**
            setFrame(convertMatToBufferedImage(frameMat));
            frame.repaint();
 
            gray.release();
 
            // **最大試行回数を超えた場合はログイン失敗**
            if (faceNotFoundCount >= MAX_ATTEMPTS) {
                System.out.println("FaceLogin: 認証失敗（時間切れ）");
                JOptionPane.showMessageDialog(frame, "認証失敗！時間切れ", "失敗", JOptionPane.ERROR_MESSAGE);
                camera.release();
                frame.dispose();
                return;
            }
        }
 
        camera.release();
    }
 
    // **カメラ映像をSwingコンポーネントに描画**
    public void setFrame(BufferedImage img) {
        this.currentFrame = img;
        repaint();
    }
 
 
 
    // **Swing の描画メソッド（カメラ映像をウィンドウに描画）**
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentFrame != null) {
            int imgWidth = currentFrame.getWidth(); // **画像の幅**
            int imgHeight = currentFrame.getHeight(); // **画像の高さ**
 
            double imgAspect = (double) imgWidth / imgHeight; // **画像のアスペクト比**
            double screenAspect = (double) screenWidth / screenHeight; // **画面のアスペクト比**
 
            int drawWidth, drawHeight;
 
            // **画面サイズとカメラ映像の比率を維持するように調整**
            if (imgAspect > screenAspect) {
                drawWidth = screenWidth;
                drawHeight = (int) (screenWidth / imgAspect);
            } else {
                drawHeight = screenHeight;
                drawWidth = (int) (screenHeight * imgAspect);
            }
 
            int x = (screenWidth - drawWidth) / 2;
            int y = (screenHeight - drawHeight) / 2;
 
            // **適切なサイズでカメラ映像を描画**
            g.drawImage(currentFrame, x, y, drawWidth, drawHeight, this);
        }
    }
 
    // **顔認証処理（登録済みの顔データと比較）**
    private static boolean isFaceMatched(Mat capturedFace) {
        File faceDir = new File(FACE_DATA_PATH);
 
        // **登録済みの顔データがない場合は認証不可**
        if (!faceDir.exists() || Objects.requireNonNull(faceDir.list()).length == 0) {
            System.out.println("FaceLogin: 登録済みの顔データがありません");
            return false;
        }
 
        double bestScore = Double.MAX_VALUE; // **最良のスコア（小さいほど類似）**
 
        // **登録済みの顔データをすべて読み込んで比較**
        for (File faceFile : Objects.requireNonNull(faceDir.listFiles())) {
            Mat registeredFace = Imgcodecs.imread(faceFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
 
            if (registeredFace.empty()) {
                System.out.println("FaceLogin: 登録済みの顔データの読み込みに失敗しました");
                continue;
            }
 
            // **キャプチャされた顔と登録顔のサイズを合わせる**
            Mat resizedFace = new Mat();
            Imgproc.resize(registeredFace, resizedFace, capturedFace.size());
 
            // **テンプレートマッチング（類似度を計算）**
            Mat result = new Mat();
            Imgproc.matchTemplate(capturedFace, resizedFace, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double similarity = mmr.maxVal; // **類似度（1.0 に近いほど一致）**
 
            // **差分スコア（数値が小さいほど類似）**
            double diff = Core.norm(capturedFace, resizedFace);
 
            // **最小のスコアを記録**
            bestScore = Math.min(bestScore, diff);
 
            System.out.println("FaceLogin: 類似度スコア = " + similarity + ", 差分スコア = " + diff);
        }
 
        // **閾値を設定し、それ未満なら認証成功**
        return bestScore < 8000;
    }
 
    // **音声を再生するメソッド**
    private static void playSound(String resourcePath) {
        try {
            // クラスローダーを使ってリソースを取得
            java.net.URL soundURL = FaceLogin.class.getResource(resourcePath);
            if (soundURL == null) {
                System.err.println("音声リソースが見つかりません: " + resourcePath);
                return;
            }
            File soundFile = new File(soundURL.toURI());

            // **音声ファイルが存在しない場合**
            if (!soundFile.exists()) {
                System.err.println("音声ファイルが見つかりません: " + soundFile.getAbsolutePath());
                return;
            }
            // **音声ファイルを読み込む**
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start(); // **音声再生開始**
            System.out.println("音声再生を開始しました: " + soundFile.getAbsolutePath());
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | java.net.URISyntaxException e) {
            System.err.println("音声再生エラー: " + e.getMessage());
        }
    }
 
    // **Mat を BufferedImage に変換**
    private static BufferedImage convertMatToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = new byte[width * height * (int) mat.elemSize()];
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, width, height, data);
        return image;
    }
 
    // **エントリーポイント（プログラムの開始）**
    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        FaceLogin faceLogin = new FaceLogin(screenSize.width, screenSize.height);
        faceLogin.start();
    }
}
 
 