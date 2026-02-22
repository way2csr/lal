package learn.lal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
public class OCRController {

    @Value("${app.ocr.tesseract-path}")
    private String tesseractPath;

    @Value("${app.ocr.languages:eng+hin}")
    private String ocrLanguages;

    @PostMapping("/ai/ocr")
    public Map<String, String> extractText(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Map.of("text", "", "error", "No file uploaded");
        }

        Path tempImageFile = null;
        Path tempOutputFile = null;

        try {
            // Create temporary file for the uploaded image
            tempImageFile = Files.createTempFile("ocr_image_", getFileExtension(file.getOriginalFilename()));
            file.transferTo(tempImageFile.toFile());

            // Create temporary file for output (without extension, tesseract adds .txt)
            tempOutputFile = Files.createTempFile("ocr_output_", "");
            String outputBasePath = tempOutputFile.toString();

            // Execute tesseract command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    tesseractPath,
                    tempImageFile.toString(),
                    outputBasePath,
                    "-l", ocrLanguages);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // Read error output
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine;
                StringBuilder errorOutput = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append("\n");
                }
                return Map.of("text", "", "error", "Tesseract failed: " + errorOutput.toString());
            }

            // Read the output file (tesseract adds .txt extension)
            Path outputFile = Path.of(outputBasePath + ".txt");
            if (Files.exists(outputFile)) {
                String extractedText = Files.readString(outputFile);
                Files.deleteIfExists(outputFile);
                return Map.of("text", extractedText.trim());
            } else {
                return Map.of("text", "", "error", "Output file not found");
            }

        } catch (IOException e) {
            return Map.of("text", "", "error", "Failed to process image: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("text", "", "error", "OCR process interrupted: " + e.getMessage());
        } finally {
            // Clean up temporary files
            try {
                if (tempImageFile != null)
                    Files.deleteIfExists(tempImageFile);
                if (tempOutputFile != null)
                    Files.deleteIfExists(tempOutputFile);
            } catch (IOException e) {
                // Log but don't fail
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null)
            return ".png";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".png";
    }
}
