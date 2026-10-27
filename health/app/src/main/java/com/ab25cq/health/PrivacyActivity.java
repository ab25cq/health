package com.ab25cq.health;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.WindowInsets;
public final class PrivacyActivity extends Activity {
    public void onCreate(Bundle b){super.onCreate(b);ScrollView s=new ScrollView(this);TextView t=new TextView(this);t.setTextSize(18);t.setPadding(28,50,28,40);t.setOnApplyWindowInsetsListener((v,i)->{var bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(28,bars.top+28,28,bars.bottom+28);return i;});
        t.setText("データの取り扱い\n\nヘルスコネクト対応万歩計\n\n利用目的\n身体活動の権限を使い、端末の歩数センサーで歩数を測定します。歩数の書き込み権限を使い、測定した歩数と測定時刻をヘルスコネクトに保存します。他のアプリの健康データは読み取りません。\n\n保存と共有\n測定記録はこの端末内とヘルスコネクトに保存します。外部サーバーへの送信、広告、第三者への提供は行いません。\n\n管理と削除\n測定は停止ボタンで終了できます。書き込み権限はヘルスコネクトの設定からいつでも取り消せます。端末内の記録はAndroidのアプリ設定でストレージを消去するか、アンインストールすると削除されます。ヘルスコネクトに同期済みの記録は、ヘルスコネクトのデータ管理画面から別途削除してください。\n\n停止中の未同期データも、権限がある場合は定期的に再送します。再送を止めるには書き込み権限を取り消してください。\n\n歩数は端末のセンサーによる推定値です。");s.addView(t);setContentView(s);}
}
