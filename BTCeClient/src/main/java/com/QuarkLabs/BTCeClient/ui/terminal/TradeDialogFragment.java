package com.QuarkLabs.BTCeClient.ui.terminal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;

import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.TradeType;
import com.QuarkLabs.BTCeClient.tasks.TradeRequest;

import java.math.BigDecimal;

public class TradeDialogFragment extends DialogFragment {

    private static final String TRADE_REQUEST_KEY = "TRADE_REQUEST";

    @Nullable
    private TradeRequestListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        TradeRequest tradeRequest = getArguments().getParcelable(TRADE_REQUEST_KEY);

        //noinspection ConstantConditions
        return new AlertDialog.Builder(getActivity())
                .setMessage(createMessage(tradeRequest))
                .setPositiveButton(android.R.string.yes,
                        (dialog, which) -> {
                            if (listener != null) {
                                listener.onSendTradeRequest(tradeRequest);
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public void setListener(@Nullable TradeRequestListener listener) {
        this.listener = listener;
    }

    @NonNull
    private Spanned createMessage(@NonNull TradeRequest request) {
        String message = null;
        final BigDecimal effectivePart = new BigDecimal("0.998");
        final BigDecimal feePart = new BigDecimal("0.002");
        if (TradeType.BUY.equals(request.getType())) {
            message = getString(R.string.buy_confirmation,
                    request.getTradeAmount(),
                    request.getTradeCurrency(),
                    new BigDecimal(request.getTradeAmount())
                            .multiply(effectivePart)
                            .stripTrailingZeros().toPlainString(),
                    request.getTradeCurrency(),
                    new BigDecimal(request.getTradeAmount())
                            .multiply(feePart)
                            .stripTrailingZeros().toPlainString(),
                    request.getTradeCurrency(),
                    request.getTradePrice(),
                    request.getTradePriceCurrency(),
                    request.getTradeCurrency(),
                    new BigDecimal(request.getTradeAmount())
                            .multiply(new BigDecimal(request.getTradePrice()))
                            .stripTrailingZeros().toPlainString(),
                    request.getTradePriceCurrency());
        } else {
            BigDecimal totalToGet = new BigDecimal(request.getTradeAmount())
                    .multiply(new BigDecimal(request.getTradePrice()));
            message = getString(R.string.sell_confirmation,
                    request.getTradeAmount(),
                    request.getTradeCurrency(),
                    request.getTradePrice(),
                    request.getTradePriceCurrency(),
                    request.getTradeCurrency(),
                    totalToGet
                            .multiply(effectivePart)
                            .stripTrailingZeros().toPlainString(),
                    request.getTradePriceCurrency(),
                    totalToGet
                            .multiply(feePart)
                            .stripTrailingZeros().toPlainString(),
                    request.getTradePriceCurrency());
        }
        return Html.fromHtml(message);
    }

    public static TradeDialogFragment create(@NonNull TradeRequest tradeRequest) {
        TradeDialogFragment dialogFragment = new TradeDialogFragment();
        dialogFragment.setCancelable(false);
        Bundle args = new Bundle();
        args.putParcelable(TRADE_REQUEST_KEY, tradeRequest);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    interface TradeRequestListener {
        void onSendTradeRequest(@NonNull TradeRequest tradeRequest);
    }
}
