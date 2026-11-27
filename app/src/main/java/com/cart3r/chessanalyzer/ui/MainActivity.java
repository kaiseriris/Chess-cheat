package com.cart3r.chessanalyzer.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.cart3r.chessanalyzer.R;
import com.cart3r.chessanalyzer.util.PermissionHelper;
import com.cart3r.chessanalyzer.viewmodel.AnalysisViewModel;

public final class MainActivity extends AppCompatActivity {
    private AnalysisViewModel vm;
    private EditText input;
    private RadioGroup modeGroup;
    private TextView best, score, depthNodes, explanation, alternatives, progress, fenView;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        vm = new ViewModelProvider(this).get(AnalysisViewModel.class);

        input        = findViewById(R.id.input);
        modeGroup    = findViewById(R.id.mode_group);
        best         = findViewById(R.id.tv_best);
        score        = findViewById(R.id.tv_score);
        depthNodes   = findViewById(R.id.tv_depth_nodes);
        explanation  = findViewById(R.id.tv_explanation);
        alternatives = findViewById(R.id.tv_alternatives);
        progress     = findViewById(R.id.tv_progress);
        fenView      = findViewById(R.id.tv_fen);

        findViewById(R.id.btn_analyze).setOnClickListener(v -> startAnalysis());
        findViewById(R.id.btn_overlay).setOnClickListener(v -> launchOverlay());

        vm.getResult().observe(this, r -> {
            if (r == null) return;
            best.setText(r.bestMoveSan);
            score.setText(r.scoreText);
            depthNodes.setText("depth " + r.depth + " · " + r.nodes + " nodes · " + r.bestMoveUci);
            explanation.setText(r.explanation);
            fenView.setText(r.fen);
            if (r.altSans.isEmpty()) {
                alternatives.setText("—");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < r.altSans.size(); i++) {
                    sb.append(i + 2).append(". ").append(r.altSans.get(i))
                      .append("  (").append(r.altScores.get(i)).append(")\n");
                }
                alternatives.setText(sb.toString().trim());
            }
        });
        vm.getProgress().observe(this, p -> progress.setText(p == null ? "" : p));
        vm.getError().observe(this, e -> { if (e != null) Toast.makeText(this, e, Toast.LENGTH_SHORT).show(); });
        vm.getBusy().observe(this, b -> {
            Button btn = findViewById(R.id.btn_analyze);
            btn.setEnabled(b == null || !b);
            btn.setText(Boolean.TRUE.equals(b) ? "Analyzing…" : "Analyze");
        });
    }

    private void startAnalysis() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) { Toast.makeText(this, "Enter FEN, PGN, or moves", Toast.LENGTH_SHORT).show(); return; }
        int mode = modeGroup.getCheckedRadioButtonId();
        int depth = 6;
        long time = 4000;
        if (mode == R.id.mode_fen)  vm.analyzeFen(text, depth, time);
        else if (mode == R.id.mode_pgn) vm.analyzePgn(text, depth, time);
        else vm.analyzeMoves(text, depth, time);
    }

    private void launchOverlay() {
        if (!PermissionHelper.canDrawOverlays(this)) {
            PermissionHelper.requestOverlayPermission(this);
            Toast.makeText(this, "Grant overlay permission, then tap again", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
        moveTaskToBack(true);
    }
}
