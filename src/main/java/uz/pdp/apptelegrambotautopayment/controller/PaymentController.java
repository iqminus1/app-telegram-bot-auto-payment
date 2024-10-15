package uz.pdp.apptelegrambotautopayment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.service.AtmosService;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PaymentController {

    private final AtmosService atmosService;
    private final CommonUtils commonUtils;

    @PostMapping("/initializeCardBinding")
    public CardBindingInitResponse initializeCardBinding(@RequestBody CardBindingInitRequest request, @RequestParam Long userId) {
        User user = commonUtils.getUser(userId);
        user.setCardNumber(request.getCardNumber());
        user.setCardExpiry(request.getExpiry());
        return atmosService.initializeCardBinding(request);
    }

    @PostMapping("/confirmCardBinding")
    public CardBindingConfirmResponse confirmCardBinding(@RequestBody CardBindingConfirmRequest request, @RequestParam Long userId) {
        CardBindingConfirmResponse cardBindingConfirmResponse = atmosService.confirmCardBinding(request);
        User user = commonUtils.getUser(userId);
        if (cardBindingConfirmResponse.getErrorCode() == null) {
            user.setCardToken(cardBindingConfirmResponse.getCardToken());
            user.setCardPhone(cardBindingConfirmResponse.getPhone());
            atmosService.autoPayment(userId);
        } else {
            user.setCardExpiry(null);
            user.setCardNumber(null);
        }
        return cardBindingConfirmResponse;
    }

    @PostMapping("/createTransaction")
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        return atmosService.createTransaction(request);
    }

    @PostMapping("/preApplyPayment")
    public PreApplyResponse preApplyPayment(@RequestBody PreApplyRequest request) {
        return atmosService.preApplyPayment(request);
    }

    @PostMapping("/applyPayment")
    public ApplyResponse applyPayment(@RequestBody ApplyRequest request) {
        return atmosService.applyPayment(request);
    }

    @PostMapping("/removeCard")
    public CardRemovalResponse removeCard(@RequestBody CardRequest cardRequest) {
        return atmosService.removeCard(cardRequest);
    }
}
