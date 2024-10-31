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
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.service.telegram.Sender;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/send")
@RequiredArgsConstructor
public class SendController {
    private final UserRepository userRepository;
    private final Sender sender;

    @PostMapping("/photo")
    public ResponseEntity<?> sendPhoto(@RequestParam String type, HttpServletRequest req) {
        List<User> users = switch (type) {
            case "all" -> userRepository.findAll();
            case "subscribed" -> userRepository.findAllBySubscribed(true);
            case "not-paid" -> userRepository.findAllBySubscribed(false);
            default -> new ArrayList<>();
        };

        try {
            Part part = req.getPart("file");
            InputStream inputStream = part.getInputStream();
            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();

            String description = req.getParameter("description");
            for (User user : users) {
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(fileBytes), part.getSubmittedFileName()));
                sendPhoto.setChatId(user.getId());

                if (description != null) {
                    sendPhoto.setCaption(description);
                }

                try {
                    sender.execute(sendPhoto);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException | ServletException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        return ResponseEntity.ok(true);
    }

    @PostMapping("/video")
    public ResponseEntity<?> sendVideo(@RequestParam String type, HttpServletRequest req) {
        List<User> users = switch (type) {
            case "all" -> userRepository.findAll();
            case "subscribed" -> userRepository.findAllBySubscribed(true);
            case "not-paid" -> userRepository.findAllBySubscribed(false);
            default -> new ArrayList<>();
        };

        try {
            Part part = req.getPart("file");
            InputStream inputStream = part.getInputStream();
            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();

            String description = req.getParameter("description");
            for (User user : users) {
                SendVideo sendVideo = new SendVideo();
                sendVideo.setVideo(new InputFile(new ByteArrayInputStream(fileBytes), part.getSubmittedFileName()));
                sendVideo.setChatId(user.getId());

                if (description != null) {
                    sendVideo.setCaption(description);
                }

                try {
                    sender.execute(sendVideo);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException | ServletException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        return ResponseEntity.ok(true);
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestParam String type, HttpServletRequest req) {
        List<User> users = switch (type) {
            case "all" -> userRepository.findAll();
            case "subscribed" -> userRepository.findAllBySubscribed(true);
            case "not-paid" -> userRepository.findAllBySubscribed(false);
            default -> new ArrayList<>();
        };

        String description = req.getParameter("description");
        if (description == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }
        for (User user : users) {
            sender.sendMessage(user.getId(), description);
        }
        return ResponseEntity.ok(true);
    }
}
