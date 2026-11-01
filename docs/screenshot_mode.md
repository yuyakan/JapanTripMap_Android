# Play ストア用スクリーンショットの撮影モード

ホーム・県詳細の YouTube 動画のサムネ・タイトル・チャンネル名を、撮影のあいだだけ
自前のダミーに差し替える（iOS 版 ScreenshotMode と同じ仕組み・同じモック画像）。

アプリ内で YouTube の動画を表示すること自体は規約上問題ないが、ストアの販促素材に
他人のサムネ（CC ライセンスではない）やチャンネル名を載せるのは別問題なので、撮影時はこれを使う。

## 使い方（デバッグ版のみ）

```
./gradlew :app:installDebug
adb shell am start -n com.kanbe1365.japantripmap/com.example.japantripmap.MainActivity --ez screenshot_mode true
```

- 撮影が終わったらアプリを終了して、普通に起動し直せば元に戻る。
- 広告も消したい場合は `AdConfig.ADS_HIDDEN` を一時的に `true` にする（撮影後は必ず戻す）。
- Shorts を開いた後の再生中の映像は YouTube のプレイヤーが描画する実物なので、撮影しないこと（iOS と同じ）。

## リリース版に混入しない仕組み

- 本体 `app/src/debug/java/.../ScreenshotMode.kt` とモック画像 `app/src/debug/res/drawable-nodpi/mock_*.jpg` は
  デバッグ版のソースセットにだけある。リリース版は `app/src/release/.../ScreenshotMode.kt`（常に無効の空実装）でビルドされる。
- リリース APK にモック画像・ダミー文言・起動フラグ名が含まれないことを確認済み。

モック画像は iOS の `tools/mock_thumbnails/build_from_bundled.py` で、同梱写真のうち
CC0 / パブリックドメインのものだけから作られたもの（人物・ロゴ・文字なし）。
