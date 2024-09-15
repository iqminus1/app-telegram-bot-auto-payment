package uz.pdp.apptelegrambotautopayment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

@Controller
@RequiredArgsConstructor
public class CardController {
    private final UserRepository userRepository;
    private final CommonUtils commonUtils;

    @GetMapping("/connect")
    public String showCardForm(Model model) {
        return "cardForm";
    }

    @PostMapping("/connect")
    public String processCardForm(@RequestParam Long userId,
                                  @RequestParam String cardNumber,
                                  @RequestParam String cardExpiry,
                                  @RequestParam String cardCvv,
                                  Model model) {
        User user = commonUtils.getUser(userId);
        user.setId(userId);
        user.setCardNumber(cardNumber);
        user.setCardExpiry(cardExpiry);
        user.setCardCvv(cardCvv);

        userRepository.save(user);

        model.addAttribute("message", "Ваша карта успешно подключена!");
        return "result";
    }
}
