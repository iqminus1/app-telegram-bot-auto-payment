package uz.pdp.apptelegrambotautopayment.service;

import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;

public interface AtmosService {
    String getToken();

    CardBindingInitResponse initializeCardBinding(CardBindingInitRequest request);

    CardBindingConfirmResponse confirmCardBinding(CardBindingConfirmRequest request);

    TransactionResponse createTransaction(TransactionRequest request);

    PreApplyResponse preApplyPayment(PreApplyRequest request);

    ApplyResponse applyPayment(ApplyRequest request);

    ApplyResponse autoPayment(Long userId);

    CardRemovalResponse removeCard(CardRequest request);
    void sendErrorMessage(Long userId,String code);
}
