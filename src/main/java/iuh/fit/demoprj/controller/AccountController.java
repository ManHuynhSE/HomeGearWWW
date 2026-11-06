package iuh.fit.demoprj.controller;

import iuh.fit.demoprj.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email,
                            @RequestParam String password, Model model) {
        try {
            if (fullName.isBlank() || email.isBlank() || password.length() < 8) {
                throw new IllegalArgumentException("Họ tên, email bắt buộc nhập; mật khẩu tối thiểu 8 ký tự.");
            }
            accountService.register(fullName, email, password);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }
}



