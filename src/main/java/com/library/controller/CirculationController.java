package com.library.controller;

import java.security.Principal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.library.domain.Loan;
import com.library.domain.Member;
import com.library.domain.WaitlistEntry;
import com.library.service.BorrowNotAllowedException;
import com.library.service.LoanNotFoundException;
import com.library.service.LoanService;
import com.library.service.MemberService;
import com.library.service.WaitlistException;
import com.library.service.WaitlistService;

/**
 * The circulation desk: borrow, return, join a waitlist, leave one. Every
 * route requires a signed-in member (SecurityConfig), so {@code principal}
 * is never null here. Thin like the other controllers — rules live in
 * LoanService and WaitlistService.
 */
@Controller
public class CirculationController {

    private static final DateTimeFormatter DUE = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final LoanService loanService;
    private final WaitlistService waitlistService;
    private final MemberService memberService;

    public CirculationController(LoanService loanService,
                                 WaitlistService waitlistService,
                                 MemberService memberService) {
        this.loanService = loanService;
        this.waitlistService = waitlistService;
        this.memberService = memberService;
    }

    @PostMapping("/books/{id}/borrow")
    public String borrow(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        Member member = memberService.getByEmail(principal.getName());
        try {
            Loan loan = loanService.borrow(id, member);
            redirect.addFlashAttribute("notice", "Borrowed \"" + loan.getBook().getTitle()
                    + "\" — due back " + DUE.format(loan.getDueDate()) + ".");
            return "redirect:/account";
        } catch (BorrowNotAllowedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/books/" + id;
        }
    }

    @PostMapping("/loans/{id}/return")
    public String returnLoan(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        Member member = memberService.getByEmail(principal.getName());
        try {
            Loan loan = loanService.returnLoan(id, member);
            redirect.addFlashAttribute("notice", "Returned \"" + loan.getBook().getTitle() + "\". Thank you.");
        } catch (LoanNotFoundException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account";
    }

    @PostMapping("/books/{id}/waitlist")
    public String joinWaitlist(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        Member member = memberService.getByEmail(principal.getName());
        try {
            WaitlistEntry entry = waitlistService.join(id, member);
            redirect.addFlashAttribute("notice", "You're #" + waitlistService.positionOf(entry)
                    + " in line for \"" + entry.getBook().getTitle() + "\".");
        } catch (WaitlistException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books/" + id;
    }

    @PostMapping("/waitlist/{id}/cancel")
    public String cancelWaitlist(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        Member member = memberService.getByEmail(principal.getName());
        try {
            WaitlistEntry entry = waitlistService.cancel(id, member);
            redirect.addFlashAttribute("notice", "Left the waitlist for \"" + entry.getBook().getTitle() + "\".");
        } catch (WaitlistException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account";
    }
}
