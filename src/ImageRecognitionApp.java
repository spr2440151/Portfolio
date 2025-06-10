import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler.LegendPosition;


public class ImageRecognitionApp extends JFrame {
    private JLabel imageLabel; // 認識対象の画像表示用
    private JScrollPane imageScrollPane; // 画像のスクロールペイン
    private JTextArea resultArea; // 認識結果のテキスト表示
    private JLabel maxLikelihoodLabel; // 最も可能性の高い動物を表示
    private ImageAnalyzer analyzer; // 画像認識ロジック
    private CameraHandler cameraHandler; // カメラ操作
    private BufferedImage currentImage; // 現在の入力画像
    private XChartPanel<CategoryChart> chartPanel; // 認識結果のグラフ
    private JScrollPane chartScrollPane; // グラフのスクロールペイン
    private static final String[] ANIMAL_NAMES = {"犬", "猫", "鳥", "ウサギ", "魚", "馬", "蛇"}; // 認識対象の動物名

    // コンストラクタ: UIとモデル画像の初期化
    public ImageRecognitionApp() {
        setTitle("動物判別プログラム");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // 全画面表示
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME); // OpenCVのロード

        imageLabel = new JLabel("ここに画像が表示されます");
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageScrollPane = new JScrollPane(imageLabel);
        imageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        imageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        maxLikelihoodLabel = new JLabel("最も可能性の高い動物: 未解析");
        maxLikelihoodLabel.setFont(new Font("Serif", Font.BOLD, 14));
        maxLikelihoodLabel.setHorizontalAlignment(JLabel.CENTER);
        
        resultArea = new JTextArea(5, 20);
        resultArea.setEditable(false);
        JButton fileButton = new JButton("画像を選択"); // ファイルから画像選択
        JButton cameraButton = new JButton("カメラで撮影"); // カメラから撮影

        // モデル画像の読み込み（各動物3枚）
        List<ImageModel> models = new ArrayList<>();
        String[] animals = {"dog", "cat", "bird", "rabbit", "fish", "horse", "snake"};
        for (int i = 0; i < animals.length; i++) {
            for (int j = 1; j <= 3; j++) {
                String path = "Modelimages/" + animals[i] + "_" + j + ".jpg";
                File file = new File(path);
                try {
                    if (file.exists()) {
                        BufferedImage img = ImageIO.read(file);
                        models.add(new ImageModel(img, ANIMAL_NAMES[i]));
                        //System.out.println("モデル画像を読み込みました: " + path);
                    } else {
                        throw new IOException("ファイルが見つかりません: " + path);
                    }
                } catch (IOException e) {
                    System.err.println("モデル画像の読み込みに失敗しました: " + e.getMessage());
                    resultArea.append("モデル画像の読み込みに失敗: " + path + "\n");
                }
            }
        }

        if (models.isEmpty()) {
            resultArea.setText("モデル画像が1つも読み込めませんでした。プログラムを終了します。");
            return;
        }

        analyzer = new ImageAnalyzer(models); // 認識器の初期化
        cameraHandler = new CameraHandler(); // カメラハンドラの初期化

        fileButton.addActionListener(e -> loadImageFromFile());
        cameraButton.addActionListener(e -> captureImageFromCamera());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(fileButton);
        buttonPanel.add(cameraButton);
        add(buttonPanel, BorderLayout.NORTH);
        add(imageScrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        southPanel.add(maxLikelihoodLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        setVisible(true);
        System.out.println("アプリケーションが初期化されました。");
    }

    // loadImageFromFile: ファイルから画像を読み込んで認識
    private void loadImageFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                currentImage = ImageIO.read(fileChooser.getSelectedFile());
                updateImageAndAnalyze();
            } catch (IOException e) {
                resultArea.setText("画像の読み込みに失敗しました。");
            }
        }
    }

    // captureImageFromCamera: カメラから画像を取得して認識
    private void captureImageFromCamera() {
        if (cameraHandler.isRunning()) {
            currentImage = cameraHandler.captureImage();
            if (currentImage != null) {
                updateImageAndAnalyze();
            } else {
                resultArea.setText("カメラから画像を取得できませんでした。");
            }
        } else {
            resultArea.setText("カメラが利用できません。接続を確認してください。");
        }
    }

    // updateImageAndAnalyze: 画像を処理し結果を表示
    private void updateImageAndAnalyze() {
        if (currentImage != null) {
            imageLabel.setIcon(new ImageIcon(currentImage));
            imageLabel.setText("");
            String analysisResult = analyzer.analyzeImage(currentImage).replace("最も可能性が高い", "最も可能性の高い動物");
            System.out.println("解析結果全文: " + analysisResult);
            resultArea.setText(analysisResult);
            updateMaxLikelihood(analysisResult);
            showXChart(analysisResult);
        } else {
            imageLabel.setIcon(null);
            imageLabel.setText("ここに画像が表示されます");
            resultArea.setText("画像がありません。");
            maxLikelihoodLabel.setText("最も可能性の高い動物: 未解析");
            clearChart();
        }
    }

    // updateMaxLikelihood: 最も可能性の高い動物をラベルに表示
    private void updateMaxLikelihood(String analysisResult) {
        String[] lines = analysisResult.split("\n");
        for (String line : lines) {
            if (line.contains("最も可能性の高い動物")) {
                maxLikelihoodLabel.setText(line);
                break;
            }
        }
    }

    // showXChart: 認識結果を棒グラフで表示
    private void showXChart(String analysisResult) {
        List<String> names = new ArrayList<>(Arrays.asList(ANIMAL_NAMES)); // 動物名リスト（固定順序）
        List<Double> values = new ArrayList<>(Collections.nCopies(names.size(), 0.0)); // 初期値0
        String[] lines = analysisResult.split("\n");

        System.out.println("解析結果の行数: " + lines.length);
        for (String line : lines) {
            System.out.println("処理中の行: " + line);
            if (line.contains(":") && !line.contains("最も可能性の高い動物")) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    try {
                        String name = parts[0].trim();
                        String valueStr = parts[1].replace("%", "").trim();
                        double value = Double.parseDouble(valueStr);
                        int index = names.indexOf(name);
                        if (index != -1) {
                            values.set(index, value);
                        } else {
                            System.err.println("未知の動物名: " + name);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("数値変換エラー: " + parts[1]);
                    }
                } else {
                    System.err.println("不正なフォーマット: " + line);
                }
            }
        }

        if (names.isEmpty() || values.isEmpty() || names.size() != values.size()) {
            System.err.println("グラフデータが不正です。names: " + names.size() + ", values: " + values.size());
            resultArea.append("\nグラフを表示できませんでした: データが不正です。");
            clearChart();
            return;
        }

        int maxIndex = 0;
        double maxValue = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) > maxValue) {
                maxValue = values.get(i);
                maxIndex = i;
            }
        }

        List<Double> normalValues = new ArrayList<>(values);
        normalValues.set(maxIndex, 0.0);
        List<Double> highlightValues = new ArrayList<>(Collections.nCopies(values.size(), 0.0));
        highlightValues.set(maxIndex, maxValue);

        int chartWidth = Math.max(400, names.size() * 60);
        CategoryChart chart = new CategoryChartBuilder()
            .width(chartWidth)
            .height(250)
            .title("動物類似度グラフ")
            .xAxisTitle("動物")
            .yAxisTitle("類似度 (%)")
            .build();

        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(100.0);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setToolTipsEnabled(true);
        chart.getStyler().setXAxisLabelRotation(45);

        chart.addSeries("その他", names, normalValues).setFillColor(Color.GRAY); // 通常棒
        chart.addSeries("最大値", names, highlightValues).setFillColor(Color.RED); // 強調棒

        System.out.println("グラフデータ (通常): " + names + " -> " + normalValues);
        System.out.println("グラフデータ (強調): " + names + " -> " + highlightValues);
        System.out.println("グラフを準備しました: 項目数=" + names.size() + ", 幅=" + chartWidth);

        if (chartScrollPane != null) {
            remove(chartScrollPane);
            chartScrollPane = null;
            chartPanel = null;
            System.out.println("前回のグラフをクリアしました。");
        }

        chartPanel = new XChartPanel<>(chart);
        chartScrollPane = new JScrollPane(chartPanel);
        chartScrollPane.setPreferredSize(new Dimension(500, 250));
        add(chartScrollPane, BorderLayout.EAST);
        revalidate();
        repaint();
        System.out.println("グラフをメインウィンドウに埋め込みました。");
    }

    // clearChart: グラフをクリア
    private void clearChart() {
        if (chartScrollPane != null) {
            remove(chartScrollPane);
            chartScrollPane = null;
            chartPanel = null;
            revalidate();
            repaint();
            System.out.println("グラフをクリアしました。");
        }
    }

    @Override
    public void dispose() {
        if (cameraHandler != null) {
            cameraHandler.release();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageRecognitionApp());
    }
}