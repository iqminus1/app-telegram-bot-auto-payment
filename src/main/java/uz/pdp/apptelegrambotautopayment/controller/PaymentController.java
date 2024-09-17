package uz.pdp.apptelegrambotautopayment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.service.AtmosService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PaymentController {

    private final AtmosService atmosService;

    @PostMapping("/initializeCardBinding")
    public CardBindingInitResponse initializeCardBinding(@RequestBody CardBindingInitRequest request) {
        return atmosService.initializeCardBinding(request);
    }

    @PostMapping("/confirmCardBinding")
    public CardBindingConfirmResponse confirmCardBinding(@RequestBody CardBindingConfirmRequest request) {
        return atmosService.confirmCardBinding(request);
    }

    @PostMapping("/createTransaction")
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        return atmosService.createTransaction(request);
    }

    @PostMapping("/preApplyPayment")
    public PreApplyResponse preApplyPayment(@RequestBody PreApplyRequest request){
        return atmosService.preApplyPayment(request);
    }
    @PostMapping("/applyPayment")
    public ApplyResponse applyPayment(@RequestBody ApplyRequest request){
        return atmosService.applyPayment(request);
    }
}
