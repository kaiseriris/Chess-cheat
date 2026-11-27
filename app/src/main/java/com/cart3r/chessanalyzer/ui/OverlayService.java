package com.cart3r.chessanalyzer.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.cart3r.chessanalyzer.R;
import com.cart3r.chessanalyzer.data.AnalysisResult;
import com.cart3r.chessanalyzer.data.EngineRepository;

public final class OverlayService extends Service {
    private static final String CHANNEL = "cj_overlay";
    private static final int NOTIF_ID = 4201;

    private WindowManager wm;
    private View root;
    private WindowManager.LayoutParams lp;
    private EngineRepository repo;
    private boolean minimized = false;
    private float touchX, touchY;
    private int startX, startY;
    private TextView tvBest, tvScore, tvAlts, tvExplain, tvStatus;
    private EditText input;
    private View content, resizeHandle;

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        startForeground(NOTIF_ID, buildNotification());
        repo = new EngineRepository();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        root = LayoutInflater.from(this).inflate(R.layout.overlay_main, null);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 80; lp.y = 200;
        wm.addView(root, lp);

        tvBest    = root.findViewById(R.id.ov_best);
        tvScore   = root.findViewById(R.id.ov_score);
        tvAlts    = root.findViewById(R.id.ov_alts);
        tvExplain = root.findViewById(R.id.ov_explain);
        tvStatus  = root.findViewById(R.id.ov_status);
        input     = root.findViewById(R.id.ov_input);
        content   = root.findViewById(R.id.ov_content);
        resizeHandle = root.findViewById(R.id.ov_resize);

        root.findViewById(R.id.ov_analyze).setOnClickListener(v -> runAnalysis());
        root.findViewById(R.id.ov_refresh).setOnClickListener(v -> runAnalysis());
        root.findViewById(R.id.ov_minimize).setOnClickListener(v -> toggleMinimize());
        root.findViewById(R.id.ov_expand).setOnClickListener(v -> expandMax());
        root.findViewById(R.id.ov_close).setOnClickListener(v -> stopSelf());

        attachDrag(root.findViewById(R.id.ov_drag_handle));
        attachResize();
    }

    private void attachDrag(View handle) {
        handle.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchX = e.getRawX(); touchY = e.getRawY();
                    startX = lp.x; startY = lp.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = startX + (int)(e.getRawX() - touchX);
                    lp.y = startY + (int)(e.getRawY() - touchY);
                    wm.updateViewLayout(root, lp);
                    return true;
            }
            return false;
        });
    }

    private void attachResize() {
        resizeHandle.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchX = e.getRawX(); touchY = e.getRawY();
                    startX = lp.width; startY = lp.height;
                    if (startX < 0) startX = root.getWidth();
                    if (startY < 0) startY = root.getHeight();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.width = Math.max(320, startX + (int)(e.getRawX() - touchX));
                    lp.height = Math.max(180, startY + (int)(e.getRawY() - touchY));
                    wm.updateViewLayout(root, lp);
                    return true;
            }
            return false;
        });
    }

    private void toggleMinimize() {
        minimized = !minimized;
        content.setVisibility(minimized ? View.GONE : View.VISIBLE);
        resizeHandle.setVisibility(minimized ? View.GONE : View.VISIBLE);
    }

    private void expandMax() {
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.x = 0;
        wm.updateViewLayout(root, lp);
    }

    private void runAnalysis() {
        String s = input.getText().toString().trim();
        if (s.isEmpty()) { Toast.makeText(this, "Enter FEN", Toast.LENGTH_SHORT).show(); return; }
        tvStatus.setText("searching…");
        repo.analyzeFen(s, 6, 4000, new EngineRepository.Callback() {
            @Override public void onProgress(int depth, int scoreCp, long nodes) {
                tvStatus.setText("d" + depth + " · " + nodes + " n");
            }
            @Override public void onComplete(AnalysisResult r) {
                tvBest.setText(r.bestMoveSan);
                tvScore.setText(r.scoreText);
                tvExplain.setText(r.explanation);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < r.altSans.size(); i++) {
                    sb.append(i + 2).append(". ").append(r.altSans.get(i))
                      .append("  (").append(r.altScores.get(i)).append(")\n");
                }
                tvAlts.setText(sb.length() == 0 ? "—" : sb.toString().trim());
                tvStatus.setText("depth " + r.depth + " · " + r.nodes + " nodes");
            }
            @Override public void onError(String message) {
                tvStatus.setText("error: " + message);
            }
        });
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Chess Overlay",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        return new NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle("CART3R Chess Analyzer")
                .setContentText("Overlay active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (root != null && wm != null) {
            try { wm.removeView(root); } catch (Exception ignored) {}
        }
        if (repo != null) repo = null;
    }
}
