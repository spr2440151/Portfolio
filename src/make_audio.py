import subprocess
import sys
import os
import requests
import zipfile
from pathlib import Path

#インストール用機能
def install_and_import(package):
    try:
        __import__(package)
    except ImportError:
        print(f"{package} が見つかりません。インストールを行います...")
        print('coution:この処理には数分かかることがあります。少々お待ちください。')
        subprocess.check_call([sys.executable, "-m", "pip", "install", package])
        __import__(package)

# gTTSとrequestsのみ必要
install_and_import("gtts")
install_and_import("requests")
from gtts import gTTS

# ffmpegを自動ダウンロードする関数
def setup_ffmpeg():
    script_dir = Path(__file__).parent
    ffmpeg_dir = script_dir / "ffmpeg_bin"
    ffmpeg_exe = ffmpeg_dir / "ffmpeg.exe"

    if not ffmpeg_exe.exists():
        print("ffmpegが見つかりません。ダウンロードします...")
        os.makedirs(ffmpeg_dir, exist_ok=True)
        
        url = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
        zip_path = ffmpeg_dir / "ffmpeg.zip"
        
        print(f"ffmpegをダウンロード中: {url}")
        response = requests.get(url)
        with open(zip_path, "wb") as f:
            f.write(response.content)
        print("ダウンロード完了")
        
        print("ZIPファイルを解凍中...")
        with zipfile.ZipFile(zip_path, "r") as zip_ref:
            for file in zip_ref.namelist():
                if "ffmpeg.exe" in file:
                    zip_ref.extract(file, ffmpeg_dir)
        print("解凍完了")

        for file in ffmpeg_dir.rglob("ffmpeg.exe"):
            file.rename(ffmpeg_exe)
            print(f"ffmpeg.exeを移動: {ffmpeg_exe}")
        
        for item in ffmpeg_dir.iterdir():
            if item != ffmpeg_exe:
                if item.is_dir():
                    import shutil
                    shutil.rmtree(item)
                else:
                    item.unlink()
        print("不要なファイルを削除しました")

    if not ffmpeg_exe.exists():
        raise FileNotFoundError(f"ffmpeg.exeが見つかりません: {ffmpeg_exe}")

    # ffmpegの動作確認
    try:
        result = subprocess.run([str(ffmpeg_exe.absolute()), "-version"], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        print(f"ffmpegが正しく設定されました: {ffmpeg_exe} (バージョン: {result.stdout.decode().splitlines()[0]})")
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"ffmpegの実行に失敗しました: {ffmpeg_exe}\nエラー: {e.stderr.decode()}")
    except FileNotFoundError:
        raise FileNotFoundError(f"ffmpegが見つかりません: {ffmpeg_exe}")

    return str(ffmpeg_exe.absolute())

# メイン処理
def main():
    ffmpeg_path = setup_ffmpeg()

    # **音声のテキスト**
    text = "ログイン成功しました"
    
    # **一時ファイルと出力ファイルの設定**
    output_folder = "resources"
    os.makedirs(output_folder, exist_ok=True)
    temp_mp3 = os.path.join(output_folder, "temp_login_success.mp3")
    output_wav = os.path.join(output_folder, "login_success.wav")
    
    # **gTTSでMP3ファイルを作成**
    tts = gTTS(text=text, lang="ja")
    tts.save(temp_mp3)
    print(f"MP3ファイルを作成: {temp_mp3}")
    
    # **ffmpegでMP3をWAVに変換 (PCM 16bit, 44.1kHz, モノラル)**
    print("MP3からWAVへの変換を開始...")
    ffmpeg_command = [
        ffmpeg_path,
        "-i", temp_mp3,           # 入力ファイル
        "-ar", "44100",          # サンプルレート 44.1kHz
        "-ac", "1",              # モノラル
        "-sample_fmt", "s16",    # PCM 16bit
        "-y",                    # 上書き許可
        output_wav               # 出力ファイル
    ]
    try:
        result = subprocess.run(ffmpeg_command, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        print(f"WAVファイルに変換: {output_wav}")
        print(f"ffmpeg出力: {result.stderr.decode()}")
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"ffmpegでの変換に失敗しました:\nエラー: {e.stderr.decode()}")

    # **一時MP3ファイルを削除**
    os.remove(temp_mp3)
    print("一時MP3ファイルを削除しました")
    
    print(f"音声ファイルが作成されました: {output_wav}")

if __name__ == "__main__":
    main()