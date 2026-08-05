package com.library.controller;

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
import com.library.service.BookForm;
import com.library.service.BookService;
import com.library.service.DuplicateIsbnException;

/**
 * Catalog pages for members and the book side of the librarian's desk.
 * Thin by design: read the request, call the service, name the template.
 */
@Controller
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
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
        model.addAttribute("books", bookService.search(q, category));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        return "catalog";
    }

    @GetMapping("/books/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getById(id));
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
