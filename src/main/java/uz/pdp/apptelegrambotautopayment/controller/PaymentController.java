package uz.pdp.apptelegrambotautopayment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import uz.pdp.apptelegrambotautopayment.dto.request.CardBindingConfirmRequest;
import uz.pdp.apptelegrambotautopayment.dto.request.CardBindingInitRequest;
import uz.pdp.apptelegrambotautopayment.dto.request.CardRequest;
import uz.pdp.apptelegrambotautopayment.dto.response.ApplyResponse;
import uz.pdp.apptelegrambotautopayment.dto.response.CardBindingConfirmResponse;
import uz.pdp.apptelegrambotautopayment.dto.response.CardBindingInitResponse;
import uz.pdp.apptelegrambotautopayment.dto.response.CardRemovalResponse;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.model.Transaction;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.TransactionRepository;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.service.AtmosService;
import uz.pdp.apptelegrambotautopayment.service.ButtonService;
import uz.pdp.apptelegrambotautopayment.service.LangService;
import uz.pdp.apptelegrambotautopayment.service.telegram.Sender;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PaymentController {

    private final AtmosService atmosService;
    private final CommonUtils commonUtils;
    private final UserRepository userRepository;
    private final Sender sender;
    private final LangService langService;
    private final TransactionRepository transactionRepository;
    private final ButtonService buttonService;

    @PostMapping("/initializeCardBinding")
    public CardBindingInitResponse initializeCardBinding(@RequestBody CardBindingInitRequest request, @RequestParam Long userId) {
        String cardNumber = request.getCardNumber();

        if (!cardNumber.matches("\\d{16}"))
            return CardBindingInitResponse.builder().errorCode(AppConstants.ERROR_TEXT).errorMessage("card exception").build();


        String expiry = request.getExpiry();
        if (!expiry.matches("^(0[1-9]|1[0-2])/(\\d{2}$)"))
            return CardBindingInitResponse.builder().errorCode(AppConstants.ERROR_TEXT).errorMessage("expire exception").build();

        if (userRepository.existsByCardNumber(cardNumber))
            return CardBindingInitResponse.builder().errorCode(AppConstants.ERROR_TEXT).errorMessage("card number exists by another user").build();

        User user = commonUtils.getUser(userId);

        String str = expiry.substring(3) + expiry.substring(0, 2);
        CardBindingInitResponse cardBindingInitResponse = atmosService.initializeCardBinding(new CardBindingInitRequest(cardNumber, str));
        if (cardBindingInitResponse.getTransactionId() != null) {
            commonUtils.setState(userId, State.SENDING_CARD_CODE);
            user.setTransactionId(cardBindingInitResponse.getTransactionId());
            user.setCardPhone(cardBindingInitResponse.getPhone());

            return cardBindingInitResponse;
        }
        cardBindingInitResponse.setCardHolder(null);
        cardBindingInitResponse.setCardToken(null);
        cardBindingInitResponse.setBalance(null);
        cardBindingInitResponse.setCardId(null);
        cardBindingInitResponse.setPan(null);
        return cardBindingInitResponse;
    }

    @PostMapping("/confirmCardBinding")
    public CardBindingConfirmResponse confirmCardBinding(@RequestBody CardBindingConfirmRequest request, @RequestParam Long userId) {
        CardBindingConfirmResponse cardBindingConfirmResponse = atmosService.confirmCardBinding(request);
        User user = commonUtils.getUser(userId);

        getPayment(userId, cardBindingConfirmResponse, user);

        cardBindingConfirmResponse.setCardHolder(null);
        cardBindingConfirmResponse.setCardToken(null);
        cardBindingConfirmResponse.setBalance(null);
        cardBindingConfirmResponse.setPan(null);
        return cardBindingConfirmResponse;
    }

    @Async
    public void getPayment(Long userId, CardBindingConfirmResponse cardBindingConfirmResponse, User user) {
        if (cardBindingConfirmResponse.getErrorCode() == null) {
            user.setCardToken(cardBindingConfirmResponse.getCardToken());
            user.setCardPhone(cardBindingConfirmResponse.getPhone());
            ApplyResponse applyResponse = atmosService.autoPayment(userId);
            user.setMethod(PaymentMethod.PAYMENT);
            if (applyResponse.getErrorMessage() == null) {
                AppConstants.setSubscriptionTime(user);
                userRepository.save(user);
                transactionRepository.save(new Transaction(applyResponse));
                sender.sendLink(userId);
            } else
                sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_ATMOS_TEXT, userId).formatted(applyResponse.getErrorMessage(), buttonService.start(userId)));
        } else
            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_ATMOS_TEXT, userId).formatted(cardBindingConfirmResponse.getErrorMessage(), buttonService.start(userId)));
    }

    //    @PostMapping("/createTransaction")
//    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
//        return atmosService.createTransaction(request);
//    }
//
//    @PostMapping("/preApplyPayment")
//    public PreApplyResponse preApplyPayment(@RequestBody PreApplyRequest request) {
//        return atmosService.preApplyPayment(request);
//    }
//
//    @PostMapping("/applyPayment")
//    public ApplyResponse applyPayment(@RequestBody ApplyRequest request) {
//        return atmosService.applyPayment(request);
//    }
//
    @PostMapping("/removeCard")
    public CardRemovalResponse removeCard(@RequestBody CardRequest cardRequest) {
        return atmosService.removeCard(cardRequest);
    }
}
