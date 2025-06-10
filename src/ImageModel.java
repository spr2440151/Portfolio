import java.awt.image.BufferedImage;


public class ImageModel {
    private BufferedImage image; // モデル画像
    private String name; // 動物名

    // コンストラクタ: 画像と名前をセット
    public ImageModel(BufferedImage image, String name) {
        this.image = image;
        this.name = name;
    }

    // ゲッターとセッター
    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}