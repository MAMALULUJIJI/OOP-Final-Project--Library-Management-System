package com.library.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.library.domain.Book;
import com.library.domain.Member;
import com.library.service.BookForm;
import com.library.service.BookService;
import com.library.service.CirculationPolicy;
import com.library.service.DuplicateIsbnException;
import com.library.service.MemberService;
import com.library.service.WaitlistService;

/**
 * Catalog pages for members and the book side of the librarian's desk.
 * Thin by design: read the request, call the service, name the template.
 */
@Controller
public class BookController {

    private final BookService bookService;
    private final WaitlistService waitlistService;
    private final MemberService memberService;

    public BookController(BookService bookService,
                          WaitlistService waitlistService,
                          MemberService memberService) {
        this.bookService = bookService;
        this.waitlistService = waitlistService;
        this.memberService = memberService;
    }

    // ---------- member-facing ----------

    /** Until a landing page exists, the catalog is the front door. */
    @GetMapping("/")
    public String home() {
        return "redirect:/catalog";
    }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String category,
                          Model model) {
        List<Book> books = bookService.search(q, category);
        // The listing used to print the raw copy count while the book page
        // printed the effective one, so a title whose last copy was promised to
        // a waitlist hold advertised itself as available and then refused to be
        // borrowed. Both pages now answer the same question.
        Map<Long, Integer> effective = new HashMap<>();
        for (Book book : books) {
            effective.put(book.getId(), waitlistService.effectiveAvailable(book));
        }
        model.addAttribute("books", books);
        model.addAttribute("effective", effective);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        return "catalog";
    }

    /**
     * The book page decides which circulation button to show, so alongside
     * the book it gets what the shelf really offers this viewer: copies not
     * reserved for waitlist holds, plus the viewer's own hold or place in
     * line when signed in.
     */
    @GetMapping("/books/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        Book book = bookService.getById(id);
        model.addAttribute("book", book);
        model.addAttribute("effectiveAvailable", waitlistService.effectiveAvailable(book));
        model.addAttribute("loanDays", CirculationPolicy.LOAN_PERIOD_DAYS);
        model.addAttribute("readyHold", null);
        model.addAttribute("activeEntry", null);
        if (principal != null) {
            Member member = memberService.getByEmail(principal.getName());
            waitlistService.readyHold(book, member)
                    .ifPresent(hold -> model.addAttribute("readyHold", hold));
            waitlistService.activeEntry(book, member).ifPresent(entry -> {
                model.addAttribute("activeEntry", entry);
                model.addAttribute("position", waitlistService.positionOf(entry));
            });
        }
        return "book";
    }

    // ---------- librarian's desk ----------

    @GetMapping("/admin")
    public String ledger(Model model) {
        model.addAttribute("books", bookService.search(null, null));
        return "admin";
    }

    @GetMapping("/admin/books/new")
    public String addBookForm(Model model) {
        model.addAttribute("bookForm", new BookForm());
        return "add-book";
    }

    @PostMapping("/admin/books")
    public String create(@Valid @ModelAttribute("bookForm") BookForm form,
                         BindingResult binding,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "add-book";
        }
        try {
            Book saved = bookService.create(form);
            redirect.addFlashAttribute("notice", "Added \"" + saved.getTitle() + "\" to the catalog.");
            return "redirect:/admin";
        } catch (DuplicateIsbnException e) {
            binding.rejectValue("isbn", "duplicate", e.getMessage());
            return "add-book";
        }
    }

    @GetMapping("/admin/books/{id}/edit")
    public String editBookForm(@PathVariable Long id, Model model) {
        Book book = bookService.getById(id);
        BookForm form = new BookForm();
        form.setTitle(book.getTitle());
        form.setAuthor(book.getAuthor());
        form.setIsbn(book.getIsbn());
        form.setCategory(book.getCategory());
        form.setTotalCopies(book.getTotalCopies());
        model.addAttribute("bookForm", form);
        model.addAttribute("bookId", id);
        return "edit-book";
    }

    @PostMapping("/admin/books/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("bookForm") BookForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("bookId", id);
            return "edit-book";
        }
        try {
            Book saved = bookService.update(id, form);
            redirect.addFlashAttribute("notice", "Updated \"" + saved.getTitle() + "\".");
            return "redirect:/admin";
        } catch (DuplicateIsbnException e) {
            binding.rejectValue("isbn", "duplicate", e.getMessage());
            model.addAttribute("bookId", id);
            return "edit-book";
        } catch (IllegalStateException e) {
            binding.rejectValue("totalCopies", "onloan", e.getMessage());
            model.addAttribute("bookId", id);
            return "edit-book";
        }
    }

    @PostMapping("/admin/books/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            bookService.delete(id);
            redirect.addFlashAttribute("notice", "Title removed from the catalog.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}
