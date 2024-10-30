package uz.pdp.apptelegrambotautopayment.service;

import org.springframework.scheduling.annotation.Async;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CompletableFuture;

public interface ProcessService {
    @Async
    CompletableFuture<Void> process(Update update);
}



