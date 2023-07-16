package dev.mv;

import dev.mv.utils.ByteUtils;
import dev.mv.utils.Utils;
import dev.mv.utils.buffer.DynamicByteBuffer;

import java.awt.image.BufferedImage;

public class ImageCompressor {
    private static final int INTERPOLATION_RADIUS = 3;
    private static final int INTERPOLATION_THRESHOLD = 5;

    public ImageCompressor() {}

    public byte[] compress(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        DynamicByteBuffer byteBuffer = new DynamicByteBuffer(8 + w * h / 2);
        byteBuffer.push(w);
        byteBuffer.push(h);

        int[] rgbaArray = image.getRGB(0, 0, w, h, null, 0, w);

        int i = 0;
        for (int rgba : rgbaArray) {
            i++;
            if (i % w == 0) {
                System.out.println("compressing --- " + Utils.getPercent(i, rgbaArray.length) + "%");
            }

            int r = (rgba >> 16) & 0xFF;
            int g = (rgba >> 8)  & 0xFF;
            int b = (rgba >> 0)  & 0xFF;
            int a = (rgba >> 24) & 0xFF;

            r = (int)Math.ceil(Utils.map(r, 0, 255, 0, 15));
            g = (int)Math.ceil(Utils.map(g, 0, 255, 0, 15));
            b = (int)Math.ceil(Utils.map(b, 0, 255, 0, 15));
            a = (int)Math.ceil(Utils.map(a, 0, 255, 0, 15));

            byte rg = (byte) ((r << 4) | g);
            byte ba = (byte) ((b << 4) | a);

            byteBuffer.push(rg);
            byteBuffer.push(ba);
        }

        return byteBuffer.array();
    }

    public BufferedImage decompress(byte[] bytes) {
        DynamicByteBuffer byteBuffer = new DynamicByteBuffer(bytes).flip();

        int width = byteBuffer.popInt();
        int height = byteBuffer.popInt();

        byte[] rgbaBytes = new byte[width * height * 4];
        System.out.println(width * height * 2);
        int i = 0;
        while (i + 8 < width * height * 4) {
            byte rg = bytes[8 + i / 2];
            byte ba = bytes[8 + i / 2 + 1];

            int r = (rg >> 4) & 0xF;
            int g = rg & 0xF;
            int b = (ba >> 4) & 0xF;
            int a = ba & 0xF;

            r = (int) Math.ceil(Utils.map((float)r, 0, 15, 0, 255));
            g = (int) Math.ceil(Utils.map((float)g, 0, 15, 0, 255));
            b = (int) Math.ceil(Utils.map((float)b, 0, 15, 0, 255));
            a = (int) Math.ceil(Utils.map((float)a, 0, 15, 0, 255));
            a = 255;

            rgbaBytes[i] = (byte) r;
            rgbaBytes[i + 1] = (byte) g;
            rgbaBytes[i + 2] = (byte) b;
            rgbaBytes[i + 3] = (byte) a;

            i += 4;
            if (i % width == 0) {
                System.out.println("decompressing --- " + Utils.getPercent(i, rgbaBytes.length) + "%");
            }
        }

        int[] colors = new int[width * height];

        i = 0;
        while (i < width * height) {
            //get avg color from adjacent pixels
            int r;
            int g;
            int b;
            int a;

            int sum;
            int count = 0;
            sum = 0;
            for (int j = -INTERPOLATION_RADIUS; j < INTERPOLATION_RADIUS; j++) {
                for (int k = -INTERPOLATION_RADIUS; k < INTERPOLATION_RADIUS; k++) {
                    try {
                        sum += ByteUtils.unsign(rgbaBytes[(i * 4 + width * 4 * j + k * 4)]);
                    } catch (IndexOutOfBoundsException ignore) {
                        sum += ByteUtils.unsign(rgbaBytes[i * 4]);
                    }
                    count++;
                }
            }
            r = (int) (sum / (float)(count));

            sum = 0;
            for (int j = -INTERPOLATION_RADIUS; j < INTERPOLATION_RADIUS; j++) {
                for (int k = -INTERPOLATION_RADIUS; k < INTERPOLATION_RADIUS; k++) {
                    try {
                        sum += ByteUtils.unsign(rgbaBytes[(i * 4 + width * 4 * j + k * 4) + 1]);
                    } catch (IndexOutOfBoundsException ignore) {
                        sum += ByteUtils.unsign(rgbaBytes[i * 4 + 1]);
                    }
                }
            }
            g = (int) (sum / (float)(count));

            sum = 0;
            for (int j = -INTERPOLATION_RADIUS; j < INTERPOLATION_RADIUS; j++) {
                for (int k = -INTERPOLATION_RADIUS; k < INTERPOLATION_RADIUS; k++) {
                    try {
                        sum += ByteUtils.unsign(rgbaBytes[(i * 4 + width * 4 * j + k * 4) + 2]);
                    } catch (IndexOutOfBoundsException ignore) {
                        sum += ByteUtils.unsign(rgbaBytes[i * 4 + 2]);
                    }
                }
            }
            b = (int) (sum / (float)(count));

            sum = 0;
            for (int j = -INTERPOLATION_RADIUS; j < INTERPOLATION_RADIUS; j++) {
                for (int k = -INTERPOLATION_RADIUS; k < INTERPOLATION_RADIUS; k++) {
                    try {
                        sum += ByteUtils.unsign(rgbaBytes[(i * 4 + width * 4 * j + k * 4) + 3]);
                    } catch (IndexOutOfBoundsException ignore) {
                        sum += ByteUtils.unsign(rgbaBytes[i * 4 + 3]);
                    }
                }
            }
            a = (int) (sum / (float)(count));

            int or = ByteUtils.unsign(rgbaBytes[i * 4]);
            int og = ByteUtils.unsign(rgbaBytes[i * 4 + 1]);
            int ob = ByteUtils.unsign(rgbaBytes[i * 4 + 2]);
            int oa = ByteUtils.unsign(rgbaBytes[i * 4 + 3]);

            int avgDiff = Math.abs(or - r);
            avgDiff += Math.abs(og - g);
            avgDiff += Math.abs(ob - b);
            avgDiff += Math.abs(oa - a);
            avgDiff /= 4;

            int rgba;
            if (avgDiff < INTERPOLATION_THRESHOLD) {
                rgba = (a << 24) | (r << 16) | (g << 8) | b;
            } else {
                rgba = (oa << 24) | (or << 16) | (og << 8) | ob;
            }
            colors[i] = rgba;

            if (i % width == 0) {
                System.out.println("interpolating edges --- " + Utils.getPercent(i, colors.length) + "%");
            }
            i++;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, colors, 0, width);
        return image;
    }
}