package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AtmosServiceImpl implements AtmosService {
    private final RestTemplate restTemplate = new RestTemplate();

}
