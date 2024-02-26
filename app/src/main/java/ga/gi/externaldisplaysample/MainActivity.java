package ga.gi.externaldisplaysample;

import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    DisplayManager displayManager;
    Map<Integer, PresentationImpl> presentations = new HashMap<>();
    int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DisplayManagerを取得
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        // ディスプレイ情報を初期化(アプリ起動時に接続されているディスプレイを取得)
        Display[] displays = updateDisplaysInfo();

        // 外部ディスプレイが接続されていた場合にPresentationを表示
        if (displays.length > 1) {
            for (Display display : displays) {
                if (display != null && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                    showPresentation(display.getDisplayId());
                }
            }
        }

        // ディスプレイ情報が変化したときのリスナーを設定
        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            private static final String TAG = "DisplayListener";

            // ディスプレイが追加された時に呼び出される
            @Override
            public void onDisplayAdded(int displayId) {
                Log.i(TAG, "onDisplayAdded: 追加");
                // 追加されたディスプレイだけに新規にPresentationを表示
                showPresentation(displayId);
            }

            // ディスプレイが切断された時に呼び出される
            @Override
            public void onDisplayRemoved(int displayId) {
                Log.i(TAG, "onDisplayRemoved: 切断");
                // 切断されたと通知されたディスプレイにPresentationが存在したかチェック
                if (presentations.containsKey(displayId)) {
                    // Presentationを取得
                    Presentation presentation = presentations.get(displayId);
                        // Presentationが取得できたか確認(Map#get()がNull許容なので)
                        if (presentation != null) {
                            // Presentationが取得できた場合はそのPresentationを破棄
                            presentation.dismiss();
                        } else {
                            // Presentationの破棄ができないのでとりあえずユーザーに通告
                            Toast.makeText(MainActivity.this, "Presentationの破棄に失敗", Toast.LENGTH_SHORT).show();
                        }
                        // Presentationを格納するMapから破棄されたPresentationそ消去
                    presentations.remove(displayId);
                }
                // onDisplayChangedが切断時には呼んでくれないのでここでディスプレイ情報を更新
                updateDisplaysInfo();
            }

            // ディスプレイ情報が更新された場合に呼び出される
            @Override
            public void onDisplayChanged(int displayId) {
                Log.i(TAG, "onDisplayChanged: 変更");
                // ディスプレイ情報を更新
                updateDisplaysInfo();
            }
        }, null);

        findViewById(R.id.countUpButton).setOnClickListener(v -> {
            // Presentationを表示できているディスプレイの数が1以上で外部ディスプレイアリと判断
            if (presentations.size() > 0) {
                count++;
                presentations.forEach((key, value) -> value.setText(String.valueOf(count)));
            } else {
                Toast.makeText(this, "外部ディスプレイが接続されていません", Toast.LENGTH_SHORT).show();
            }
        });
    }

    Display[] updateDisplaysInfo() {
        // DisplayManagerからDisplayを取得
        Display[] displays = displayManager.getDisplays();
        // とりあえずユーザーに接続されているディスプレイの数を通告
        Toast.makeText(this, "ディスプレイの数: " + displays.length, Toast.LENGTH_SHORT).show();

        TextView displaysView = findViewById(R.id.displaysView);
        String displaysInfo = "";

        for (Display value : displays) {
            // 接続されているすべてのディスプレイの情報を取得
            // 馬鹿みたいに情報量が多いのでスペースを空けさせる
            displaysInfo = displaysInfo.concat(value + "\n\n");
        }

        // ディスプレイ情報を表示
        displaysView.setText(displaysInfo);
        return displays;
    }

    void showPresentation(int displayId) {
        // displayIdからDisplayを取得
        Display display = displayManager.getDisplay(displayId);
        // displayが正常に取得できているか確認
        if (display != null) {
            // Mapに該当するdisplayIdのデータが無いか確認
            if (presentations.containsKey(displayId)) {
                // Presentationを取得
                Presentation presentation = presentations.get(displayId);
                // Presentationが取得できたか確認(Map#get()がNull許容なので)
                if (presentation != null) {
                    // Presentationが取得できた場合はそのPresentationを破棄
                    presentation.dismiss();
                } else {
                    // Presentationの破棄ができないのでとりあえずユーザーに通告
                    Toast.makeText(MainActivity.this, "Presentationの破棄に失敗", Toast.LENGTH_SHORT).show();
                }
            }
            // 新規にPresentationを作成
            PresentationImpl presentation = new PresentationImpl(this, display);
            // 作成したPresentationを該当するディスプレイに表示
            presentation.show();
            // 作成したPresentationをMapに登録
            presentations.put(displayId, presentation);
        }

    }

    private class PresentationImpl extends Presentation {
        int displayId;

        public PresentationImpl(Context outerContext, Display display) {
            super(outerContext, display);
            // DisplayIdを外付けディスプレイ側で表示するので、事前にコンストラクタで取得
            displayId = display.getDisplayId();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.external_main);

            TextView idView = findViewById(R.id.externalDisplayIdView);
            // DisplayIdを外付けディスプレイに表示
            idView.setText(getString(R.string.id, displayId));
        }

        // 外部クラスからPresentation内のTextViewを操作するためのメソッド
        // 直接MainActivityから操作しようとしてもfindViewById()がnullを返す
        void setText(String text) {
            TextView view = findViewById(R.id.externalTextView);
            view.setText(text);
        }
    }
}