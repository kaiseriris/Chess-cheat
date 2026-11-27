package com.cart3r.chessanalyzer.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cart3r.chessanalyzer.data.AnalysisResult;
import com.cart3r.chessanalyzer.data.EngineRepository;

public final class AnalysisViewModel extends ViewModel {
    private final EngineRepository repo = new EngineRepository();

    private final MutableLiveData<AnalysisResult> result = new MutableLiveData<>();
    private final MutableLiveData<String> progress = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    public LiveData<AnalysisResult> getResult() { return result; }
    public LiveData<String> getProgress() { return progress; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getBusy() { return busy; }

    public void analyzeFen(String fen, int depth, long timeMs) {
        run(() -> repo.analyzeFen(fen, depth, timeMs, cb));
    }
    public void analyzePgn(String pgn, int depth, long timeMs) {
        run(() -> repo.analyzePgn(pgn, depth, timeMs, cb));
    }
    public void analyzeMoves(String moves, int depth, long timeMs) {
        run(() -> repo.analyzeMoves(moves, depth, timeMs, cb));
    }

    private void run(Runnable r) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.postValue(true);
        error.postValue(null);
        progress.postValue("Searching…");
        r.run();
    }

    private final EngineRepository.Callback cb = new EngineRepository.Callback() {
        @Override public void onProgress(int depth, int scoreCp, long nodes) {
            progress.postValue("depth " + depth + " · " + String.format(java.util.Locale.US, "%.2f", scoreCp / 100.0) + " · " + nodes + " nodes");
        }
        @Override public void onComplete(AnalysisResult r) {
            result.postValue(r);
            busy.postValue(false);
            progress.postValue(null);
        }
        @Override public void onError(String message) {
            error.postValue(message);
            busy.postValue(false);
            progress.postValue(null);
        }
    };
}
