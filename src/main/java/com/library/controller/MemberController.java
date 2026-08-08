package com.library.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.library.domain.Member;
import com.library.service.DuplicateEmailException;
import com.library.service.MemberForm;
import com.library.service.MemberService;

/**
 * The member side of the librarian's desk: the register and the
 * issue-a-card form. Same shape as BookController — read, call, name a
 * template — and no rules of its own.
 */
@Controller
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/admin/members")
    public String register(Model model) {
        model.addAttribute("members", memberService.list());
        return "members";
    }

    @GetMapping("/admin/members/new")
    public String newMemberForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        return "register-member";
    }

    @PostMapping("/admin/members")
    public String create(@Valid @ModelAttribute("memberForm") MemberForm form,
                         BindingResult binding,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "register-member";
        }
        try {
            Member saved = memberService.register(form);
            redirect.addFlashAttribute("notice",
                    "Library card issued to " + saved.getName() + ".");
            return "redirect:/admin/members";
        } catch (DuplicateEmailException e) {
            binding.rejectValue("email", "duplicate", e.getMessage());
            return "register-member";
        }
    }

    @PostMapping("/admin/members/{id}/suspend")
    public String suspend(@PathVariable Long id, RedirectAttributes redirect) {
        Member member = memberService.suspend(id);
        redirect.addFlashAttribute("notice", member.getName() + " suspended.");
        return "redirect:/admin/members";
    }

    @PostMapping("/admin/members/{id}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes redirect) {
        Member member = memberService.reactivate(id);
        redirect.addFlashAttribute("notice", member.getName() + " reactivated.");
        return "redirect:/admin/members";
    }
}
