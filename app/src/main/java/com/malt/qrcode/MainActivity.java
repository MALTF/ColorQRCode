package com.malt.qrcode;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;

/**
 * @author maliang
 */
public class MainActivity extends AppCompatActivity {

    ImageView qrCodeImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrCodeImageView = findViewById(R.id.qrcode_image_view);

        findViewById(R.id.create_qrcode_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Bitmap colorQrCode = EncoderQrCode.with().encodeQrCode(new String("https://github.com/MALTF".getBytes(StandardCharsets.UTF_8)),
                            0.85f, 256, new int[]{Color.parseColor("#02E06D"), Color.WHITE}, 1);
                    qrCodeImageView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (colorQrCode != null) {
                                qrCodeImageView.setImageBitmap(colorQrCode);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}