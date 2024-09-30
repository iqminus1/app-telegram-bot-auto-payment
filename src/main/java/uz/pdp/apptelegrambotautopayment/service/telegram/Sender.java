package uz.pdp.apptelegrambotautopayment.service.telegram;

import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class Sender extends DefaultAbsSender {

    public Sender() {
        super(new DefaultBotOptions(), AppConstants.BOT_TOKEN);
    }

    public void sendMessage(Long userId, String text) {
        try {
            execute(new SendMessage(userId.toString(), text));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageWithMarkdown(Long userId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage(userId.toString(), text);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setParseMode("Markdown");
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(Long userId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage(userId.toString(), text);
        sendMessage.setReplyMarkup(replyKeyboard);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void leaveChat(Long groupId) {
        try {
            executeAsync(new LeaveChat(groupId.toString()));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void openChat(Long userId, Long groupId) {
        acceptJoinRequest(userId, groupId);
        kickChatMember(userId, groupId);
    }

    public void acceptJoinRequest(Long userId, Long groupId) {
        ApproveChatJoinRequest acceptJoinReq = new ApproveChatJoinRequest();
        acceptJoinReq.setUserId(userId);
        acceptJoinReq.setChatId(groupId);
        try {
            execute(acceptJoinReq);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void kickChatMember(Long userId, Long groupId) {
        try {
            String string = groupId.toString();
            BanChatMember banChatMember = new BanChatMember(string, userId);
            execute(banChatMember);
            UnbanChatMember unbanChatMember = new UnbanChatMember(string, userId);
            execute(unbanChatMember);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLink(Long groupId) {
        try {
            EditChatInviteLink editChatInviteLink = new EditChatInviteLink();
            editChatInviteLink.setChatId(groupId);
            editChatInviteLink.setCreatesJoinRequest(true);
            editChatInviteLink.setName("Link by bot");
            ChatInviteLink execute = execute(editChatInviteLink);
            return execute.getInviteLink();
        } catch (TelegramApiException e) {
            try {
                CreateChatInviteLink createChatInviteLink = new CreateChatInviteLink();
                createChatInviteLink.setName("Link by bot");
                createChatInviteLink.setCreatesJoinRequest(true);
                createChatInviteLink.setChatId(groupId);

                return execute(createChatInviteLink).getInviteLink();
            } catch (TelegramApiException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public boolean checkChatMember(Long userId, Long groupId) {
        try {
            return !execute(new GetChatMember(groupId.toString(), userId)).getStatus().equals("left");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFilePath(PhotoSize photoSize) {
        GetFile getFile = new GetFile(photoSize.getFileId());
        try {
            File execute = execute(getFile);

            String fileUrl = execute.getFileUrl(AppConstants.BOT_TOKEN);

            String fileName = UUID.randomUUID().toString();
            String[] split = fileUrl.split("\\.");
            String fileExtension = split[split.length - 1];
            String filePath = fileName + "." + fileExtension;

            Path targetPath = Paths.get(AppConstants.FILE_PATH, filePath);

            Files.createDirectories(targetPath.getParent());

            try (InputStream inputStream = new URL(fileUrl).openStream();
                 OutputStream outputStream = Files.newOutputStream(targetPath)) {
                StreamUtils.copy(inputStream, outputStream);
            }

            return targetPath.toString();
        } catch (TelegramApiException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Chat getChat(Long userId) {
        try {
            return execute(new GetChat(userId.toString()));
        } catch (TelegramApiException e) {
            Chat chat = new Chat();
            chat.setId(userId);
            chat.setUserName("topilmadi");
            chat.setFirstName("topilmadi");
            return chat;
        }
    }

    public void sendPhoto(Long userId, String caption, String path, InlineKeyboardMarkup keyboard) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setCaption(caption);
        InputFile photo = new InputFile();
        photo.setMedia(new java.io.File(path));
        sendPhoto.setPhoto(photo);
        sendPhoto.setChatId(userId);
        sendPhoto.setReplyMarkup(keyboard);
        executeAsync(sendPhoto);
    }

    public void changeCaption(Long userId, Integer messageId, String text) {
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(userId);
        editMessageCaption.setCaption(text);
        editMessageCaption.setMessageId(messageId);
        try {
            execute(editMessageCaption);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
