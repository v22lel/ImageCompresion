package dev.mv;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        ImageCompressor compressor = new ImageCompressor();

        BufferedImage img = ImageIO.read(new File("src/res/img2.png"));
        byte[] compressed = compressor.compress(img);
        writeCMP(compressed);

        Path path = Path.of("src/res/cmp.ci");
        byte[] compressedBytes = Files.readAllBytes(path);
        BufferedImage image = compressor.decompress(compressedBytes);
        writeIMG(image);
    }

    private static void writeCMP(byte[] compressed) {
        try (FileOutputStream fos = new FileOutputStream("src/res/cmp.ci")) {
            fos.write(compressed);
            System.out.println("File written successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing the file: " + e.getMessage());
        }
    }

    private static void writeIMG(BufferedImage image) {
        try {
            File outputFile = new File("src/res/out.png");
            ImageIO.write(image, "png", outputFile);
            System.out.println("Image saved successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while saving the image: " + e.getMessage());
        }
    }
}
