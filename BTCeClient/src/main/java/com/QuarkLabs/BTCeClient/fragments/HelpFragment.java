/*
 * BTC-e client
 *     Copyright (C) 2014  QuarkDev Solutions <quarkdev.solutions@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.QuarkLabs.BTCeClient.fragments;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.QuarkLabs.BTCeClient.R;

public class HelpFragment extends Fragment implements View.OnClickListener {

    private static final String APP_EMAIL_ADDRESS = "quarkdev.solutions@gmail.com";
    private static final String APP_EMAIL_SUBJECT = "Feedback on BTC-e client for Android";
    private final static String APP_PNAME = "com.QuarkLabs.BTCeClient";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        @SuppressWarnings("ConstantConditions") TextView aboutText = (TextView) getView().findViewById(R.id.aboutText);
        aboutText.setText(Html.fromHtml(getResources().getString(R.string.AboutText)));
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());

        Button sendFeedbackButton = (Button) getView().findViewById(R.id.sendFeedbackButton);
        Button rateAppButton = (Button) getView().findViewById(R.id.rateAppButton);
        sendFeedbackButton.setOnClickListener(this);
        rateAppButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rateAppButton:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + APP_PNAME)));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + APP_PNAME)));
                }
                break;
            case R.id.sendFeedbackButton:
                Intent sendToIntent = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", APP_EMAIL_ADDRESS, null))
                        .putExtra(Intent.EXTRA_SUBJECT, APP_EMAIL_SUBJECT);
                startActivity(Intent.createChooser(sendToIntent, "Send email"));
                break;
            default:
                break;
        }
    }
}
