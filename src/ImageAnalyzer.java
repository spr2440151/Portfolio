import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.features2d.*;
import java.awt.image.BufferedImage;
import java.util.*;


public class ImageAnalyzer {
    private List<ImageModel> models; // 認識用モデル画像のリスト

    // コンストラクタ: モデル画像をセット
    public ImageAnalyzer(List<ImageModel> models) {
        this.models = models;
    }

    // analyzeImage: 入力画像をモデルと比較し、結果を文字列で返す
    public String analyzeImage(BufferedImage inputImage) {
        if (inputImage == null || models == null || models.isEmpty()) {
            return "エラー: 画像またはモデルが読み込まれていません。";
        }

        Mat inputMat = preprocessImage(bufferedImageToMat(inputImage)); // 前処理済み入力画像
        Map<String, List<Double>> similarityMap = new HashMap<>(); // 動物ごとの類似度リスト
        for (ImageModel model : models) {
            Mat modelMat = preprocessImage(bufferedImageToMat(model.getImage()));
            double similarity = compareImages(inputMat, modelMat); // ORBで比較
            similarityMap.computeIfAbsent(model.getName(), k -> new ArrayList<>()).add(similarity);
        }

        StringBuilder result = new StringBuilder("類似度解析結果:\n");
        double maxSimilarity = -1;
        String maxModelName = "";

        // 各動物の平均類似度を計算
        for (String name : similarityMap.keySet()) {
            List<Double> similarities = similarityMap.get(name);
            double avgSimilarity = similarities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double similarityPercent = Math.max(0, Math.min(100, avgSimilarity * 100));
            if (similarityPercent > maxSimilarity) {
                maxSimilarity = similarityPercent;
                maxModelName = name;
            }
            result.append(name).append(": ").append(String.format("%.2f", similarityPercent)).append("%\n");
        }

        result.insert(0, "最も可能性が高い: **" + maxModelName + "** (" + String.format("%.2f", maxSimilarity) + "%)\n\n");
        return result.toString();
    }

    // bufferedImageToMat: BufferedImageをOpenCVのMat形式に変換
    private Mat bufferedImageToMat(BufferedImage image) {
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        byte[] data = new byte[image.getWidth() * image.getHeight() * 3];
        int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int i = 0; i < rgb.length; i++) {
            data[i * 3 + 2] = (byte) (rgb[i] & 0xFF); // 青
            data[i * 3 + 1] = (byte) ((rgb[i] >> 8) & 0xFF); // 緑
            data[i * 3] = (byte) ((rgb[i] >> 16) & 0xFF); // 赤
        }
        mat.put(0, 0, data);
        return mat;
    }

    // preprocessImage: 画像の前処理（ノイズ除去と正規化）
    private Mat preprocessImage(Mat img) {
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGB2GRAY); // グレースケール変換
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0); // ノイズ除去
        Core.normalize(gray, gray, 0, 255, Core.NORM_MINMAX); // 正規化
        return gray;
    }

    // compareImages: ORB特徴量で画像を比較し類似度を返す
    private double compareImages(Mat img1, Mat img2) {
        if (img1.size().width != img2.size().width || img1.size().height != img2.size().height) {
            Imgproc.resize(img2, img2, img1.size()); // サイズを統一
        }

        ORB detector = ORB.create();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        detector.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
        detector.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);

        if (descriptors1.empty() || descriptors2.empty()) {
            return 0.0; // 特徴点がない場合
        }

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);

        List<DMatch> matchesList = matches.toList();
        double totalDistance = 0;
        for (DMatch match : matchesList) {
            totalDistance += match.distance;
        }
        return matchesList.isEmpty() ? 0 : (1 - totalDistance / (matchesList.size() * 100.0)); // 類似度計算
    }

    public List<ImageModel> getModels() {
        return models;
    }

    public void setModels(List<ImageModel> models) {
        this.models = models;
    }
}