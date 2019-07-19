package com.htc.wallet.skrsdk.verification.uilistener;

import android.util.Pair;

import java.util.Map;

public interface RecyclerViewItemClickListener {
    void onItemCheck(Map<Integer, Pair<String, Map<String, String>>> item);

    void onItemUnCheck(Map<Integer, Pair<String, Map<String, String>>> item);
}
