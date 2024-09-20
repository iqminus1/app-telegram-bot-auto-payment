package uz.pdp.apptelegrambotautopayment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.model.Group;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.GroupRepository;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtmosServiceImpl implements AtmosService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CommonUtils commonUtils;
    private final GroupRepository groupRepository;
    private final Sender sender;
    private final LangService langService;
    private String token = null;
    private long tokenExpirationTime = 0;

    @Override
    public String getToken() {
        if (token == null || System.currentTimeMillis() >= tokenExpirationTime) {
            try {
                List<Group> groups = groupRepository.findAll();
                if (token == null) {
                    if (groups.size() == 1) {
                        token = groups.get(0).getToken();
                        tokenExpirationTime = groups.get(0).getExpireAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (token != null)
                            return getToken();
                    }
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.setBasicAuth(AppConstants.CLIENT_ID, AppConstants.CLIENT_SECRET);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("grant_type", "client_credentials");
                if (token != null)
                    body.add("refresh_token", token);

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                        AppConstants.ATMOS_AUTH_URL, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                        });
                log.info(response.toString());
                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, String> bodyMap = response.getBody();
                    if (bodyMap != null) {
                        token = bodyMap.get("access_token");
                        tokenExpirationTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 час
                        if (groups.size() == 1) {
                            Group group = groups.get(0);
                            group.setToken(token);
                            group.setExpireAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(tokenExpirationTime), ZoneId.systemDefault()));
                            groupRepository.save(group);
                        }
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

    //if error phone is code
    @Override
    public CardBindingInitResponse initializeCardBinding(CardBindingInitRequest request) {
        log.info(LocalDateTime.now().toString());
        log.info("Card init kirdi");
        log.info(request.toString());
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_BIND_CARD_INIT_URL, HttpMethod.POST, entity, String.class);
        log.info(response.toString());
        log.info(LocalDateTime.now().toString());
        log.info("Card init Javob oldi \n\n");
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode data = objectMapper.readTree(response.getBody());
                JsonNode result = data.get("result");
                String code = result.get("code").asText();
                String errorMessage = result.get("description").asText();
                if (code.startsWith("STPIMS-ERR-")) {
                    return CardBindingInitResponse.builder().errorCode(code.substring(AppConstants.ERROR_LENGTH)).errorMessage(errorMessage).build();
                }
                String transactionId = data.get("transaction_id").asText();
                String text = data.get("phone").asText();
                return CardBindingInitResponse.builder().transactionId(transactionId).phone(text).build();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Ошибка при инициализации привязки карты: " + response.getStatusCode());
        }
    }


    //if error phone is code
    @Override
    public CardBindingConfirmResponse confirmCardBinding(CardBindingConfirmRequest request) {
        log.info(LocalDateTime.now().toString());
        log.info("Card confirm kirdi");
        log.info(request.toString());
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_BIND_CARD_CONFIRM_URL, HttpMethod.POST, entity, String.class);
        log.info(response.toString());
        log.info(LocalDateTime.now().toString());
        log.info("Card confirm Javob oldi \n\n");

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode result = jsonNode.get("result");
                String code = result.get("code").asText();
                String errorMessage = result.get("description").asText();
                if (code.startsWith("STPIMS-ERR-"))
                    return CardBindingConfirmResponse.builder().errorCode(code.substring(AppConstants.ERROR_LENGTH)).errorMessage(errorMessage).build();
                JsonNode dataNode = jsonNode.get("data");
                return objectMapper.treeToValue(dataNode, CardBindingConfirmResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            sender.sendMessage(5182943798L, response.toString());
            throw new RuntimeException("Ошибка при подтверждении привязки карты: " + response.getStatusCode());
        }
    }

    @Override
    public TransactionResponse createTransaction(TransactionRequest request) {
        log.info(LocalDateTime.now().toString());
        log.info("create transaction kirdi");
        log.info(request.toString());
        log.info(request.toString());
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_CREATE_TRANSACTION_URL, HttpMethod.POST, entity, String.class);

        log.info(response.toString());
        log.info(LocalDateTime.now().toString());
        log.info("transaction Javob oldi \n\n");

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String text = jsonNode.get("result").get("code").asText();
            String errorMessage = jsonNode.get("result").get("description").asText();
            if (text.startsWith(AppConstants.ERROR_TEXT))
                return TransactionResponse.builder().errorCode(text.substring(AppConstants.ERROR_LENGTH)).errorMessage(errorMessage).build();
            String transactionId = jsonNode.get("transaction_id").asText();
            JsonNode storeTransaction = jsonNode.get("store_transaction");
            long amount = storeTransaction.get("amount").asLong();
            long userId = storeTransaction.get("account").asLong();
            return TransactionResponse.builder().transactionId(transactionId).amount(amount).userId(userId).build();
        } catch (JsonProcessingException e) {
            sender.sendMessage(5182943798L, response.toString());
            sender.sendMessage(5182943798L, e.toString());
            throw new RuntimeException(e);
        }
    }

    @Override
    public PreApplyResponse preApplyPayment(PreApplyRequest request) {
        log.info(LocalDateTime.now().toString());
        log.info("pre apply kirdi");
        log.info(request.toString());
        log.info(request.toString());
        HttpEntity<String> entity = getHttpEntity(request);

        restTemplate.exchange(
                AppConstants.ATMOS_PRE_APPLY_URL, HttpMethod.POST, entity, String.class);
        log.info("Pre apply ge bordi\n\n");


        return new PreApplyResponse(request.getTransactionId());
    }

    @Override
    public ApplyResponse applyPayment(ApplyRequest request) {
        log.info(LocalDateTime.now().toString());
        log.info("apply  kirdi");
        log.info(request.toString());
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_APPLY_URL, HttpMethod.POST, entity, String.class);
        log.info(response.toString());
        log.info(LocalDateTime.now().toString());
        log.info("apply Javob oldi \n\n");
        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String text = jsonNode.get("result").get("code").asText();
            String errorMessage = jsonNode.get("result").get("description").asText();
            if (text.startsWith(AppConstants.ERROR_TEXT)) {
                return ApplyResponse.builder().errorCode(text.substring(AppConstants.ERROR_LENGTH)).errorMessage(errorMessage).build();
            }
            JsonNode storeTransaction = jsonNode.get("store_transaction");
            String successTransId = storeTransaction.get("success_trans_id").asText();
            String transId = storeTransaction.get("trans_id").asText();
            long userId = storeTransaction.get("account").asLong();
            long amount = storeTransaction.get("amount").asLong();
            return ApplyResponse.builder().successTransId(successTransId).transId(transId).userId(userId).amount(amount).build();
        } catch (JsonProcessingException e) {
            sender.sendMessage(5182943798L, response.toString());
            sender.sendMessage(5182943798L, e.toString());
            throw new RuntimeException(e);
        }
    }

    @Override
    public ApplyResponse autoPayment(Long userId) {
        User user = commonUtils.getUser(userId);
        TransactionResponse transaction = createTransaction(new TransactionRequest(userId));
        String transactionId = transaction.getTransactionId();
        preApplyPayment(new PreApplyRequest(transactionId, user.getCardToken()));
        return applyPayment(new ApplyRequest(transactionId));
    }

    @Override
    public CardRemovalResponse removeCard(CardRequest request) {
        HttpEntity<String> entity = getHttpEntity(request);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.ATMOS_REMOVE_CARD_URL, HttpMethod.POST, entity, String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode removeResponse = jsonNode.get("data");
            return objectMapper.treeToValue(removeResponse, CardRemovalResponse.class);
        } catch (JsonProcessingException e) {
            sender.sendMessage(5182943798L, response.toString());
            sender.sendMessage(5182943798L, e.toString());
            throw new RuntimeException(e);
        }

    }

    @Override
    public void sendErrorMessage(Long userId, String code) {
        if (code.equals("003")) {
            sender.sendMessage(userId, langService.getMessage(LangFields.ERROR_003, userId));
            return;
        }
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


//
//STPIMS-ERR-001
//STPIMS-ERR-005
//STPIMS-ERR-008
//
//
//STPIMS-ERR-057
//STPIMS-ERR-067
//
//
//STPIMS-ERR-098
//STPIMS-ERR-102
//STPIMS-ERR-117

