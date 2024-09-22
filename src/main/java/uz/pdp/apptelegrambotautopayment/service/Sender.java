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
}
