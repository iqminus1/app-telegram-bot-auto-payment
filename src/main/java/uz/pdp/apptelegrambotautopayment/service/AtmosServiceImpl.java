package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AtmosServiceImpl implements AtmosService {

    private final RestTemplate restTemplate;
    private String token = null;
    private long tokenExpirationTime = 0;

    @Override
    public String getToken() {
        if (token == null || System.currentTimeMillis() >= tokenExpirationTime) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth(AppConstants.CLIENT_ID, AppConstants.CLIENT_SECRET);
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        AppConstants.ATMOS_AUTH_URL, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, String> body = response.getBody();
                    if (body != null) {
                        token = body.get("access_token");
                        tokenExpirationTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 час
                    }
                } else {
                    token = null;
                }
            } catch (Exception e) {
                token = null;
            }
        }
        return token;
    }

    @Override
    public CardBindingInitResponse initializeCardBinding(CardBindingInitRequest request) {
        HttpEntity<CardBindingInitRequest> entity = getHttpEntity(request);

        ResponseEntity<CardBindingInitResponse> response = restTemplate.exchange(
                AppConstants.ATMOS_BIND_CARD_INIT_URL, HttpMethod.POST, entity, CardBindingInitResponse.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("Ошибка при инициализации привязки карты: " + response.getStatusCode());
        }
    }

    @Override
    public CardBindingConfirmResponse confirmCardBinding(CardBindingConfirmRequest request) {
        HttpEntity<CardBindingConfirmRequest> entity = getHttpEntity(request);

        ResponseEntity<CardBindingConfirmResponse> response = restTemplate.exchange(
                AppConstants.ATMOS_BIND_CARD_CONFIRM_URL, HttpMethod.POST, entity, CardBindingConfirmResponse.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("Ошибка при подтверждении привязки карты: " + response.getStatusCode());
        }
    }

    @Override
    public TransactionResponse createTransaction(TransactionRequest request) {
        HttpEntity<TransactionRequest> entity = getHttpEntity(request);

        ResponseEntity<TransactionResponse> response = restTemplate.exchange(
                AppConstants.ATMOS_CREATE_TRANSACTION_URL, HttpMethod.POST, entity, TransactionResponse.class);

        return response.getBody();
    }

    @Override
    public PreApplyResponse preApplyPayment(PreApplyRequest request) {
        HttpEntity<PreApplyRequest> entity = getHttpEntity(request);

        ResponseEntity<PreApplyResponse> response = restTemplate.exchange(
                AppConstants.ATMOS_PRE_APPLY_URL, HttpMethod.POST, entity, PreApplyResponse.class);

        return response.getBody();
    }

    @Override
    public ApplyResponse applyPayment(ApplyRequest request) {
        HttpEntity<ApplyRequest> entity = getHttpEntity(request);

        ResponseEntity<ApplyResponse> response = restTemplate.exchange(
                AppConstants.ATMOS_APPLY_URL, HttpMethod.POST, entity, ApplyResponse.class);

        return response.getBody();
    }

    @Override
    public CardRemovalResponse removeCard(CardRequest request) {
        HttpEntity<CardRequest> entity = getHttpEntity(request);

        ResponseEntity<CardRemovalResponse> response = restTemplate.exchange(
                AppConstants.ATMOS_REMOVE_CARD_URL, HttpMethod.POST, entity, CardRemovalResponse.class);

        return response.getBody();
    }

    private <T> HttpEntity<T> getHttpEntity(T request) {
        String token = getToken();
        if (token == null) {
            throw new IllegalStateException("Не удалось получить токен.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }
}
