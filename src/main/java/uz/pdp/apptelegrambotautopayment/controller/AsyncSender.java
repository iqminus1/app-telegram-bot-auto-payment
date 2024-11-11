package uz.pdp.apptelegrambotautopayment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.service.telegram.Sender;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@RequiredArgsConstructor
public class AsyncSender {

    private final Sender sender;
    private final UserRepository userRepository;

    @Bean(name = "taskExecutorCustom")
    public Executor taskExecutorCustom() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("AsyncSender-");
        executor.initialize();
        return executor;
    }

    @Async("taskExecutorCustom")
    public void sendAsyncPhotoOrVideo(String fileName, byte[] fileBytes, String description, String type, boolean isPhoto) {
        int pageNumber = 0;
        boolean hasNextPage;
        String path = null;
        int size = 0;
        int count = 0;
        do {
            Page<User> users = getUsers(type, pageNumber);
            if (users.isEmpty()) break;
            List<User> content = users.getContent();
            size = size + content.size();
            if (isPhoto) {
                SendPhoto sendPhoto = new SendPhoto();
                if (path == null)
                    sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(fileBytes), fileName));
                else sendPhoto.setPhoto(new InputFile(path));
                if (description != null)
                    sendPhoto.setCaption(description);
                sendPhoto.setParseMode(ParseMode.HTML);
                for (User user : content) {
                    sendPhoto.setChatId(user.getId());
                    try {
                        Message execute = sender.execute(sendPhoto);
                        count++;
                        if (path == null) {
                            path = execute.getPhoto().stream().max(Comparator.comparing(PhotoSize::getFileSize)).orElseThrow().getFileId();
                            sendPhoto.setPhoto(new InputFile(path));
                        }
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                SendVideo sendVideo = new SendVideo();
                if (path == null)
                    sendVideo.setVideo(new InputFile(new ByteArrayInputStream(fileBytes), fileName));
                else sendVideo.setVideo(new InputFile(path));
                if (description != null)
                    sendVideo.setCaption(description);
                sendVideo.setParseMode(ParseMode.HTML);
                for (User user : users) {
                    sendVideo.setChatId(user.getId());
                    try {
                        Message execute = sender.execute(sendVideo);
                        count++;
                        if (path == null) {
                            path = execute.getPhoto().stream().max(Comparator.comparing(PhotoSize::getFileSize)).orElseThrow().getFileId();
                            sendVideo.setVideo(new InputFile(path));
                        }
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }

            hasNextPage = users.hasNext();
            pageNumber++;
        }
        while (hasNextPage);
        sender.sendMessage(727977552L, "Userlar soni %s \nQabul qilganlar soni %s".formatted(size, count));
    }


    @Async("taskExecutorCustom")
    public void sendAsyncMessage(String description, String type) {
        int pageNumber = 0;
        int count = 0;
        int size = 0;
        boolean hasNextPage;
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(description);
        sendMessage.setParseMode(ParseMode.HTML);
        do {
            Page<User> users = getUsers(type, pageNumber);
            List<User> content = users.getContent();
            size = size + content.size();
            for (User user : content) {
                sendMessage.setChatId(user.getId());
                try {
                    sender.execute(sendMessage);
                    count++;
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            pageNumber++;
            hasNextPage = users.hasNext();
        } while (hasNextPage);
        sender.sendMessage(727977552L, "Userlar soni %s \nQabul qilganlar soni %s".formatted(size, count));
    }

    private Page<User> getUsers(String type, int pageNumber) {
        Sort sort = Sort.sort(User.class).by(User::getId).ascending();
        Pageable pageable = PageRequest.of(pageNumber, 100, sort);
        return switch (type) {
            case "all" -> userRepository.findAll(pageable);
            case "subscribed" -> userRepository.findAllBySubscribed(true, pageable);
            case "not-paid" -> userRepository.findAllBySubscribed(false, pageable);
            default -> Page.empty();
        };
    }
}
