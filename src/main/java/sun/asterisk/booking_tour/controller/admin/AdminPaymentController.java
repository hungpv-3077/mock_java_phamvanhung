package sun.asterisk.booking_tour.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    @GetMapping
    public String payments(Model model) {
        model.addAttribute("activePage", "payments");
        model.addAttribute("pageTitle", "Payment Notifications");
        return "admin/payments";
    }
}
