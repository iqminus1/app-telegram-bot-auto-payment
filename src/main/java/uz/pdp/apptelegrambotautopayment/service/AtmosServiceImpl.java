package uz.pdp.apptelegrambotautopayment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AtmosServiceImpl implements AtmosService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private String token = null;
    private long tokenExpirationTime = 0;

    @Override
    public String getToken() {
        if (token == null || System.currentTimeMillis() >= tokenExpirationTime) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.setBasicAuth(AppConstants.CLIENT_ID, AppConstants.CLIENT_SECRET);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("grant_type", "client_credentials");

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                        AppConstants.ATMOS_AUTH_URL, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                        });

                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, String> bodyMap = response.getBody();
                    if (bodyMap != null) {
                        token = bodyMap.get("access_token");
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
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_BIND_CARD_INIT_URL, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode data = objectMapper.readTree(response.getBody());
                String transactionId = data.get("transaction_id").asText();
                String text = data.get("phone").asText();
                return new CardBindingInitResponse(transactionId, text);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Ошибка при инициализации привязки карты: " + response.getStatusCode());
        }
    }

    @Override
    public CardBindingConfirmResponse confirmCardBinding(CardBindingConfirmRequest request) {
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_BIND_CARD_CONFIRM_URL, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode dataNode = jsonNode.get("data");
                return objectMapper.treeToValue(dataNode, CardBindingConfirmResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Ошибка при подтверждении привязки карты: " + response.getStatusCode());
        }
    }

    @Override
    public TransactionResponse createTransaction(TransactionRequest request) {
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_CREATE_TRANSACTION_URL, HttpMethod.POST, entity, String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String transactionId = jsonNode.get("transaction_id").asText();
            JsonNode storeTransaction = jsonNode.get("store_transaction");
            long amount = storeTransaction.get("amount").asLong();
            long userId = storeTransaction.get("account").asLong();
            return new TransactionResponse(transactionId, amount, userId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PreApplyResponse preApplyPayment(PreApplyRequest request) {
        HttpEntity<String> entity = getHttpEntity(request);

        restTemplate.exchange(
                AppConstants.ATMOS_PRE_APPLY_URL, HttpMethod.POST, entity, String.class);


        return new PreApplyResponse(request.getTransactionId());
    }

    @Override
    public ApplyResponse applyPayment(ApplyRequest request) {
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_APPLY_URL, HttpMethod.POST, entity, String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode storeTransaction = jsonNode.get("store_transaction");
            String successTransId = storeTransaction.get("success_trans_id").asText();
            String transId = storeTransaction.get("trans_id").asText();
            long userId = storeTransaction.get("account").asLong();
            long amount = storeTransaction.get("amount").asLong();
            return new ApplyResponse(successTransId, transId, userId, amount);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CardRemovalResponse removeCard(CardRequest request) {
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<CardRemovalResponse> response = restTemplate.exchange(
                AppConstants.ATMOS_REMOVE_CARD_URL, HttpMethod.POST, entity, CardRemovalResponse.class);

        return response.getBody();
    }

    private <T> HttpEntity<String> getHttpEntity(T request) {
        String token = getToken();
        if (token == null) {
            throw new IllegalStateException("Не удалось получить токен.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map map = objectMapper.convertValue(request, Map.class);
        String json = null;
        try {
            json = objectMapper.writeValueAsString(map);
            return new HttpEntity<>(json, headers);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
