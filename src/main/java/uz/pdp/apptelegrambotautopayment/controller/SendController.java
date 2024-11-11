package uz.pdp.apptelegrambotautopayment.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/v1/send")
@RequiredArgsConstructor
public class SendController {
    private final AsyncSender asyncSender;

    @PostMapping("/photo")
    public ResponseEntity<?> sendAsyncPhoto(@RequestParam String type, HttpServletRequest req) {
        try {
            Part part = req.getPart("file");
            InputStream inputStream = part.getInputStream();
            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();
            String description = req.getParameter("description");
            asyncSender.sendAsyncPhotoOrVideo(part.getSubmittedFileName(), fileBytes, description, type, true);
            return ResponseEntity.ok(true);
        } catch (IOException | ServletException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }

    @PostMapping("/video")
    public ResponseEntity<?> sendVideo(@RequestParam String type, HttpServletRequest req) {
        try {
            Part part = req.getPart("file");
            InputStream inputStream = part.getInputStream();
            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();
            String description = req.getParameter("description");
            asyncSender.sendAsyncPhotoOrVideo(part.getSubmittedFileName(), fileBytes, description, type, false);
            return ResponseEntity.ok(true);
        } catch (IOException | ServletException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestParam String type, HttpServletRequest req) {
        String description = req.getParameter("description");
        asyncSender.sendAsyncMessage(description, type);
        return ResponseEntity.ok(true);

    }


}

