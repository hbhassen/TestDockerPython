package com.example.controller;

import com.example.config.UploadProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final long MAX_CONTENT_LENGTH = 10L * 1024 * 1024; // 10 Mo

    private final UploadProperties uploadProperties;

    public UploadController(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @PostMapping
    public String upload(HttpServletRequest request) {
        try {
            String transferEncoding = request.getHeader("Transfer-Encoding");
            String contentLengthHeader = request.getHeader("Content-Length");

            boolean isChunked = "chunked".equalsIgnoreCase(transferEncoding);
            long contentLength = contentLengthHeader != null ? Long.parseLong(contentLengthHeader) : -1;

            // Règle de refus si content-length > 10Mo et non chunked
            if (!isChunked && contentLength > MAX_CONTENT_LENGTH) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content-Length too large");
            }

            // Lecture du corps
            byte[] data = StreamUtils.copyToByteArray(request.getInputStream());

            // Enregistrement
            File uploadDir = new File(uploadProperties.getDirectory());
            if (!uploadDir.exists()) uploadDir.mkdirs();

            File savedFile = new File(uploadDir, UUID.randomUUID() + ".bin");
            try (FileOutputStream out = new FileOutputStream(savedFile)) {
                out.write(data);
            }

            return "Fichier enregistré : " + savedFile.getName();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de traitement", e);
        }
    }
}