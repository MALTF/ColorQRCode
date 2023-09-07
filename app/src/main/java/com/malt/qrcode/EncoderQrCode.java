package com.malt.qrcode;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.FloatRange;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author maliang
 */
public class EncoderQrCode {

    private static volatile EncoderQrCode encoderQrCode;

    public static EncoderQrCode with() {
        if (null == encoderQrCode) {
            synchronized (EncoderQrCode.class) {
                if (null == encoderQrCode) {
                    encoderQrCode = new EncoderQrCode();
                }
            }
        }
        return encoderQrCode;
    }

    /**
     * 创建二维码
     *
     * @param content 二维码内容
     * @param qR      圆角
     * @param side    宽高相同
     * @param colors  颜色 二维码颜色、背景颜色 多个颜色的话自行配置吧这里是在代码里写死的
     * @param padding 二维码距离边框的距离 默认是Math.min(outputWidth / (originalWidth + 2), outputHeight / (originalHeight + 2)) 如果设置0 则没有边框
     *                需要注意的是 当padding设置为0是 会导致识别的速度变慢
     * @return 二维码Bitmap
     * @throws Exception e
     */
    public Bitmap encodeQrCode(String content, @FloatRange(from = 0, to = 1.0) float qR, int side, int[] colors, int padding) throws Exception {
        int outputWidth = side, outputHeight = side;
        int qrColor = colors[0], bgColor = colors[1];

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8);
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        QRCode qrCode = Encoder.encode(content, ErrorCorrectionLevel.H, hints);

        ByteMatrix matrix = qrCode.getMatrix();
        int originalWidth = matrix.getWidth();
        int originalHeight = matrix.getHeight();

        outputWidth = Math.max(originalWidth, outputWidth);
        outputHeight = Math.max(originalHeight, outputHeight);

        int originalPadding = Math.min(outputWidth / (originalWidth + 2), outputHeight / (originalHeight + 2));
        if (padding > 0) {
            padding = Math.max(padding, originalPadding);
        }

        int cellWidth = Math.min((outputWidth - padding * 2) / originalWidth, (outputHeight - padding * 2) / originalHeight);

        int outputLeft = 0, outputTop = 0;
        if (padding > 0) {
            outputLeft = (outputWidth - cellWidth * originalWidth) / 2;
            outputTop = (outputHeight - cellWidth * originalHeight) / 2;
        }

        int cellMid = cellWidth / 2;
        int R = (int) (cellMid * qR);

        // 方块大小
        int blockSize = originalWidth / 7;
        // 方块偏移量
        int offset = (int) (blockSize * 1.8);
        // 探测图形的大小
        int detectCornerSize = matrix.get(0, 5) == 1 ? 7 : 5;

        int[] positions = getAlignmentPatternPositions(qrCode.getVersion());
        StringBuilder sb = new StringBuilder();
        for (int a : positions) {
            sb.append("p:").append(a).append("  ");
            sb.append("px:").append(a * (outputWidth / originalWidth)).append("  ");
        }
        Log.d("positions", String.format("length:%s positions:%s", positions.length, sb));

        Paint qrPaint = new Paint();
        qrPaint.setColor(qrColor);
        qrPaint.setStyle(Paint.Style.FILL);
        qrPaint.setAntiAlias(true);

        Bitmap bitmap = Bitmap.createBitmap(padding == 0 ? outputWidth - (outputWidth - cellWidth * originalWidth) : outputWidth,
                padding == 0 ? outputHeight - (outputHeight - cellWidth * originalHeight) : outputHeight, Bitmap.Config.ARGB_8888);

        bitmap.eraseColor(bgColor);
        Canvas canvas = new Canvas(bitmap);
        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                int pX = x - 1;
                int nX = x + 1;
                int pY = y - 1;
                int nY = y + 1;
                int brL = outputLeft + x * cellWidth;
                int brT = outputTop + y * cellWidth;
                int brR = brL + cellWidth;
                int brB = brT + cellWidth;
                boolean l = pX >= 0 && matrix.get(pX, y) == 1;
                boolean t = pY >= 0 && matrix.get(x, pY) == 1;
                boolean r = nX < originalWidth && matrix.get(nX, y) == 1;
                boolean b = nY < originalHeight && matrix.get(x, nY) == 1;
                if (matrix.get(x, y) == 1) {
                    // 绘制定位方块
                    if (x < offset && y < offset) {
                        // 左上角方块
                        if (inOuterDetectCornerArea(x, y, originalWidth, originalHeight, detectCornerSize)) {
                            qrPaint.setColor(Color.parseColor("#00A5FF"));
                        } else {
                            qrPaint.setColor(Color.parseColor("#FF6B36"));
                        }
                    } else if (x < offset && y >= originalHeight - offset) {
                        // 左下角
                        qrPaint.setColor(Color.parseColor("#FF6B36"));
                    } else if (x >= originalWidth - offset && y < offset) {
                        // 右上角方块
                        if (inOuterDetectCornerArea(x, y, originalWidth, originalHeight, detectCornerSize)) {
                            qrPaint.setColor(Color.parseColor("#AC0D00"));
                        } else {
                            qrPaint.setColor(Color.parseColor("#FF6B36"));
                        }
                    } else {
                        if (x <= positions[0] + 2 && x >= positions[0] - 2 && y >= positions[1] - 1 && y <= positions[1] + 3) {
                            qrPaint.setColor(Color.parseColor("#00A5FF"));
                        } else {
                            qrPaint.setColor(qrColor);
                        }
                    }

                    boolean tl = !(t || l || (pX >= 0 && pY >= 0 && matrix.get(pX, pY) == 1));
                    boolean tr = !(t || r || (nX < originalWidth && pY >= 0 && matrix.get(nX, pY) == 1));
                    boolean br = !(b || r || (nX < originalWidth && nY < originalHeight && matrix.get(nX, nY) == 1));
                    boolean bl = !(b || l || (pX >= 0 && nY < originalHeight && matrix.get(pX, nY) == 1));
                    Path path = new Path();
                    if (tl) {
                        path.moveTo(brL, brT + R);
                        // 在路径上添加圆弧段
                        path.arcTo(new RectF(brL, brT, brL + 2 * R, brT + 2 * R), -180, 90, false);
                    } else {
                        path.moveTo(brL, brT);
                    }
                    if (tr) {
                        path.lineTo(brR - R, brT);
                        path.arcTo(new RectF(brR - 2 * R, brT, brR, brT + 2 * R), -90, 90, false);
                    } else {
                        path.lineTo(brR, brT);
                    }
                    if (br) {
                        path.lineTo(brR, brB - R);
                        path.arcTo(new RectF(brR - 2 * R, brB - 2 * R, brR, brB), 0, 90, false);
                    } else {
                        path.lineTo(brR, brB);
                    }
                    if (bl) {
                        path.lineTo(brL + R, brB);
                        path.arcTo(new RectF(brL, brB - 2 * R, brL + 2 * R, brB), 90, 90, false);
                    } else {
                        path.lineTo(brL, brB);
                    }
                    path.close();
                    canvas.drawPath(path, qrPaint);
                } else {
                    if (t && l) {
                        Path path = new Path();
                        path.moveTo(brL, brT + R);
                        path.lineTo(brL, brT);
                        path.lineTo(brL + R, brT);
                        path.arcTo(new RectF(brL, brT, brL + 2 * R, brT + 2 * R), -90, -90, false);
                        path.close();
                        canvas.drawPath(path, qrPaint);
                    }
                    if (t && r) {
                        Path path = new Path();
                        path.moveTo(brR - R, brT);
                        path.lineTo(brR, brT);
                        path.lineTo(brR, brT + R);
                        path.arcTo(new RectF(brR - 2 * R, brT, brR, brT + 2 * R), 0, -90, false);
                        path.close();
                        canvas.drawPath(path, qrPaint);
                    }
                    if (b && r) {
                        Path path = new Path();
                        path.moveTo(brR, brB - R);
                        path.lineTo(brR, brB);
                        path.lineTo(brR - R, brB);
                        path.arcTo(new RectF(brR - 2 * R, brB - 2 * R, brR, brB), 90, -90, false);
                        path.close();
                        canvas.drawPath(path, qrPaint);
                    }
                    if (b && l) {
                        Path path = new Path();
                        path.moveTo(brL + R, brB);
                        path.lineTo(brL, brB);
                        path.lineTo(brL, brB - R);
                        path.arcTo(new RectF(brL, brB - 2 * R, brL + 2 * R, brB), 180, -90, false);
                        path.close();
                        canvas.drawPath(path, qrPaint);
                    }
                }
            }
        }
        return bitmap;
    }

    public int[] getAlignmentPatternPositions(Version version) {
        int[] alignmentPatternCenters = version.getAlignmentPatternCenters();
        int numPatterns = alignmentPatternCenters.length;
        StringBuilder sb = new StringBuilder();
        for (int a : alignmentPatternCenters) {
            sb.append("center:").append(a).append("  ");
        }
        Log.d("alignmentPatternCenters", String.format("length:%s positions:%s", numPatterns, sb));
        int[] positions = new int[numPatterns];
        int position = getAlignmentPatternPosition(version);
        int skip = (numPatterns == 2) ? 1 : (numPatterns - 1) * 4 / (numPatterns + 1);

        for (int i = 0; i < numPatterns; i++) {
            positions[i] = position;
            position -= skip;
        }

        return positions;
    }

    public int getAlignmentPatternPosition(Version version) {
        if (version.getVersionNumber() < 2) {
            return 0;
        } else {
            int dimension = version.getDimensionForVersion();
            return dimension - 7;
        }
    }

    /**
     * 判断 (x,y) 对应的点是否为二维码举证探测图形中外面的框, 这个方法的调用必须在确认(x,y)对应的点在探测图形内
     *
     * @param x                目标点的x坐标
     * @param y                目标点的y坐标
     * @param matrixW          二维码矩阵宽
     * @param matrixH          二维码矩阵高
     * @param detectCornerSize 探测图形的大小
     * @return 对应的点在探测图形内
     */
    private boolean inOuterDetectCornerArea(int x, int y, int matrixW, int matrixH, int detectCornerSize) {
        // true外层的框 false内层方块
        return x == 0 || x == detectCornerSize - 1 || x == matrixW - 1 || x == matrixW - detectCornerSize || y == 0 ||
                y == detectCornerSize - 1 || y == matrixH - 1 || y == matrixH - detectCornerSize;
    }
}
