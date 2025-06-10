import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;


public class FaceApp extends JFrame {
    private JLabel titleLabel; // タイトル表示用のラベル
    private RoundedButton loginButton; // ログインボタン
    private RoundedButton registerButton; // 顔登録ボタン
    private float titleAlpha = 0.0f; // タイトルの透明度（フェードイン用）
    private int loginX = -200; // ログインボタンの初期X位置（スライドイン用）
    private int registerX = 600; // 顔登録ボタンの初期X位置（スライドイン用）
    private Timer fadeTimer; // フェードインアニメーションのタイマー
    private Timer slideTimer; // スライドインアニメーションのタイマー

    // コンストラクタ: アプリの初期設定とUI構築
    public FaceApp() {
        setTitle("FaceApp - 顔認証システム"); // ウィンドウタイトル
        setSize(400, 250); // ウィンドウサイズ調整
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 閉じるボタンで終了
        setLocationRelativeTo(null); // 画面中央に配置
        setLayout(null); // アニメーションのために絶対位置指定

        // OpenCVライブラリのロード（顔認証とカメラ機能に必要）
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);

        // 起動時にmake_audio.pyを実行（srcディレクトリで動作させる）
        File audioFile = new File("src/resources/login_success.wav");
        if (!audioFile.exists()) {
            try {
                // ProcessBuilderでカレントディレクトリをsrcに設定
                ProcessBuilder pb = new ProcessBuilder("python", "make_audio.py");
                pb.directory(new File("src")); // 実行ディレクトリをsrcに変更
                Process process = pb.start();
                process.waitFor(); // スクリプトの終了を待つ
                System.out.println("make_audio.pyをsrcディレクトリで実行しました。");
            } catch (IOException | InterruptedException e) {
                System.err.println("make_audio.pyの実行に失敗しました: " + e.getMessage());
            }
        } else {
            System.out.println("login_success.wavが既に存在するため、make_audio.pyは実行しません。");
        }

        // 背景をグラデーションに設定
        setContentPane(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setPaint(new GradientPaint(0, 0, new Color(0, 191, 255), // 青
                                               0, getHeight(), new Color(0, 51, 102))); // 濃い青
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        });
        getContentPane().setLayout(null);

        // タイトルラベルを非表示で配置（paintで直接描画するため）
        titleLabel = new JLabel("FaceAppへようこそ", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(0, 30, 400, 40);
        titleLabel.setVisible(false); // 非表示にして重複防止
        add(titleLabel);

        // ログインボタン（角丸デザイン）
        loginButton = new RoundedButton("ログイン");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setBackground(new Color(50, 205, 50)); // ライムグリーン
        loginButton.setForeground(Color.WHITE);
        loginButton.setBounds(loginX, 100, 120, 40);
        loginButton.addActionListener(e -> startLogin());
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(new Color(60, 255, 60)); // ホバーで明るく
            }
            @Override
            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(new Color(50, 205, 50));
            }
        });
        add(loginButton);

        // 顔登録ボタン（角丸デザイン）
        registerButton = new RoundedButton("顔登録");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        registerButton.setBackground(new Color(255, 165, 0)); // オレンジ
        registerButton.setForeground(Color.WHITE);
        registerButton.setBounds(registerX, 160, 120, 40);
        registerButton.addActionListener(e -> startRegisterFace());
        registerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                registerButton.setBackground(new Color(255, 195, 0)); // ホバーで明るく
            }
            @Override
            public void mouseExited(MouseEvent e) {
                registerButton.setBackground(new Color(255, 165, 0));
            }
        });
        add(registerButton);

        // タイトルフェードインアニメーション
        fadeTimer = new Timer(50, e -> {
            titleAlpha += 0.05f; // 透明度を徐々に上げる
            if (titleAlpha >= 1.0f) {
                titleAlpha = 1.0f;
                fadeTimer.stop();
            }
            repaint(); // 再描画でアニメーション反映
        });
        fadeTimer.start();

        // ボタンスライドインアニメーション
        slideTimer = new Timer(20, e -> {
            if (loginX < 140) loginX += 10; // 左から中央へ
            if (registerX > 140) registerX -= 10; // 右から中央へ
            loginButton.setLocation(loginX, 100);
            registerButton.setLocation(registerX, 160);
            if (loginX >= 140 && registerX <= 140) {
                slideTimer.stop();
            }
        });
        slideTimer.start();

        setVisible(true);
    }

    // paint: タイトルを直接描画しフェードイン効果を適用
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, titleAlpha));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth("FaceAppへようこそ");
        int x = (getWidth() - textWidth) / 2;
        int y = 60;
        g2d.drawString("FaceAppへようこそ", x, y);
        g2d.dispose();
    }

    // startLogin: ログインアプリを起動
    private void startLogin() {
        dispose(); // FaceAppウィンドウを閉じる
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        new Thread(() -> {
            FaceLogin faceLogin = new FaceLogin(screenWidth, screenHeight);
            faceLogin.start();
        }).start();
    }

    // startRegisterFace: 顔登録アプリを起動
    private void startRegisterFace() {
        dispose();
        new Thread(() -> {
            RegisterFace registerFace = new RegisterFace();
            registerFace.start();
        }).start();
    }

    // main: アプリの起動場所
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FaceApp());
    }

    // RoundedButton: カスタムボタンで角丸デザインを実現
    class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setOpaque(false); // デフォルト背景を透明に
            setContentAreaFilled(false);
            setFocusPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isArmed()) {
                g2.setColor(getBackground().darker()); // 押下時に暗く
            } else {
                g2.setColor(getBackground());
            }
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20); // 角丸背景
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(getText());
            int textHeight = fm.getAscent();
            int x = (getWidth() - textWidth) / 2;
            int y = (getHeight() + textHeight) / 2 - fm.getDescent();
            g2.drawString(getText(), x, y); // テキストを中央に
            g2.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20); // 角丸ボーダー
            g2.dispose();
        }

        @Override
        public void setContentAreaFilled(boolean b) {
            // デフォルトの四角い背景を無効化
        }
    }
}