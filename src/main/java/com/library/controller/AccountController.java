package com.library.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.library.domain.Member;
import com.library.service.CirculationPolicy;
import com.library.service.LoanService;
import com.library.service.MemberService;
import com.library.service.WaitlistService;

/**
 * The member's own page: current loans, waitlist standings, and the full
 * borrowing ledger (proposal 2.4, "View Borrowing History").
 */
@Controller
public class AccountController {

    private final MemberService memberService;
    private final LoanService loanService;
    private final WaitlistService waitlistService;

    public AccountController(MemberService memberService,
                             LoanService loanService,
                             WaitlistService waitlistService) {
        this.memberService = memberService;
        this.loanService = loanService;
        this.waitlistService = waitlistService;
    }

    @GetMapping("/account")
    public String account(Principal principal, Model model) {
        Member member = memberService.getByEmail(principal.getName());
        model.addAttribute("member", member);
        model.addAttribute("loans", loanService.historyFor(member));
        model.addAttribute("waitlists", waitlistService.standingsFor(member));
        model.addAttribute("holdDays", CirculationPolicy.HOLD_PERIOD_DAYS);
        return "account";
    }
}
