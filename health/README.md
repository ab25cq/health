# ヘルスコネクト対応万歩計

パッケージ: `com.ab25cq.health`。compileSdk / targetSdk 36、minSdk 34（Android 14以降）。Android標準APIのみを使用します。

## 使い方

1. APKをインストールして起動します。
2. 「開始」を押し、身体活動・ヘルスコネクトの歩数書き込み・通知の権限を許可します。
3. スマートフォンを携帯して歩きます。通知を表示したhealth型フォアグラウンドサービスが、画面を閉じた後も測定します。
4. 約15分間隔と停止時に同期します。「今すぐ同期」でも送信できます。

書き込み権限のみ使用し、他アプリの歩数は読み取りません。画面の歩数はこのアプリ自身の測定分です。センサー非搭載端末では測定できません。

## 記録・再送

TYPE_STEP_COUNTERの累積値の差をSQLiteに保存します。開始時の最初の読み取りは基準値です。停止中の歩数は次の開始に含めません。再起動時は基準値をリセットし、測定中だった場合は再開を試みます。サービスがシステムに終了された場合はSTART_STICKYによる再開を試みます。

通常の読み取りは約1分の記録にまとめ、Health Connectへ開始・終了時刻と歩数、端末情報を送信します。固定clientRecordIdと増加するclientRecordVersionにより、通信完了直後にプロセスが終了しても再送で重複しません。同期成功時に送信済みとし、失敗時は保存したままJobSchedulerでも再試行します。読み取り用のバックグラウンド権限は不要です。

同期間隔は目安です。Dozeやメーカーの省電力制御によって遅延・停止する場合があります。ユーザーが強制停止した場合はアプリで再度開始してください。電源OFF中の測定はできません。センサーの遅延配信が日付をまたぐ場合、画面の当日歩数は終了時刻基準で計上されます。

## ビルド

JDK 17、Android SDK 36を使用します。`local.properties`のSDKパスを環境に合わせてください。

```
./gradlew assembleRelease bundleRelease lintRelease
```

署名には親ディレクトリの`my-release-key.jks`と、Git管理対象外の`signing.properties`（storePassword / keyPassword / keyAlias）を使用します。キーやパスワードをリポジトリに含めないでください。

成果物:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Google Playへ公開する場合は、Play Consoleでヘルスコネクトの歩数書き込みとhealth型フォアグラウンドサービスの利用目的を申告し、公開用プライバシーポリシーを設定してください。

## 検証

署名付きrelease APK/AABの生成、Android Lint、APK署名の検証を実施。接続端末がないため、実機での歩数センサー・権限ダイアログ・Health Connectへの反映は未検証です。

実機では、開始→画面OFFで歩行→15分以上経過→Health Connectでこのアプリ由来の歩数を確認してください。停止後の歩数除外、書き込み権限取消後の未同期保存と再許可後の再送、端末再起動後の測定再開も確認項目です。

参照: [health型サービス](https://developer.android.com/develop/background-work/services/fgs/service-types#health)、[Health Connect書き込み](https://developer.android.com/health-and-fitness/health-connect/write-data)。
