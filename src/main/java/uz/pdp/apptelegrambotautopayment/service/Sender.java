package uz.pdp.apptelegrambotautopayment.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;

import java.util.Random;
import java.util.UUID;

@Component
public class Sender extends DefaultAbsSender {
    private final Random random;

    public Sender(Random random) {
        super(new DefaultBotOptions(), AppConstants.BOT_TOKEN);
        this.random = random;
    }

    public void sendMessage(Long userId, String text) {
        try {
            execute(new SendMessage(userId.toString(), text));
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

    public void deleteInviteLink(Long groupId, String link) {
        try {
            executeAsync(new RevokeChatInviteLink(groupId.toString(), link));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLink(Long groupId) {
        try {
            return createLink(groupId);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Не удалось создать или обновить ссылку на приглашение", e);
        }
    }

    private String createLink(Long groupId) throws TelegramApiException {
        CreateChatInviteLink createChatInviteLink = new CreateChatInviteLink();
        createChatInviteLink.setChatId(groupId);
        createChatInviteLink.setCreatesJoinRequest(true);
        createChatInviteLink.setName(UUID.randomUUID().toString().substring(random.nextInt(1, 7), random.nextInt(10, 32)));
        ChatInviteLink execute = execute(createChatInviteLink);
        return execute.getInviteLink();
    }
}
