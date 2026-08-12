package com.library.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.domain.Book;
import com.library.domain.WaitlistStatus;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.WaitlistRepository;

/**
 * Business rules for the catalog. All validation beyond simple field checks
 * lives here; controllers call these methods and never touch the repository.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository books;
    private final LoanRepository loans;
    private final WaitlistRepository waitlist;

    public BookService(BookRepository books, LoanRepository loans, WaitlistRepository waitlist) {
        this.books = books;
        this.loans = loans;
        this.waitlist = waitlist;
    }

    /**
     * Catalog search. Blank query and blank category mean "everything";
     * "all" is what the category chips send for the same thing.
     */
    public List<Book> search(String query, String category) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasCategory = category != null && !category.isBlank()
                && !"all".equalsIgnoreCase(category);

        if (hasQuery) {
            String q = query.trim();
            List<Book> matches =
                    books.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(q, q);
            if (hasCategory) {
                matches = matches.stream()
                        .filter(b -> category.equalsIgnoreCase(b.getCategory()))
                        .toList();
            }
            return matches;
        }
        if (hasCategory) {
            return books.findByCategoryIgnoreCase(category);
        }
        return books.findAll();
    }

    public Book getById(Long id) {
        return books.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    /** A brand-new title starts with every copy on the shelf. */
    @Transactional
    public Book create(BookForm form) {
        if (books.existsByIsbn(form.getIsbn().trim())) {
            throw new DuplicateIsbnException(form.getIsbn());
        }
        Book book = new Book();
        applyForm(book, form);
        book.setAvailableCopies(book.getTotalCopies());
        return books.save(book);
    }

    /**
     * Copies on loan are untouchable: the new total may not drop below the
     * number currently out, and available is recomputed rather than edited.
     */
    @Transactional
    public Book update(Long id, BookForm form) {
        Book book = getById(id);

        books.findByIsbn(form.getIsbn().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateIsbnException(form.getIsbn());
                });

        // Copies promised to a READY hold are as spoken-for as copies already
        // out: the member has been told one is waiting. Counting only loans
        // here let a librarian shrink the total onto a reserved copy, and the
        // holder collecting it drove availableCopies to -1.
        int onLoan = book.getCopiesOnLoan();
        int reserved = (int) waitlist.countByBookAndStatus(book, WaitlistStatus.READY);
        int committed = onLoan + reserved;
        if (form.getTotalCopies() < committed) {
            throw new IllegalStateException(
                    onLoan + " copies are on loan and " + reserved
                            + " are held for the waitlist; total copies cannot go below "
                            + committed + ".");
        }

        applyForm(book, form);
        book.setAvailableCopies(book.getTotalCopies() - onLoan);
        return book;
    }

    /**
     * A title cannot leave the catalog while any copy is out with a member.
     * Checked twice: the derived count, and the loan table itself as the
     * backstop. A removed title takes its closed loans and its waitlist
     * with it — the rows reference the book and cannot outlive it.
     */
    @Transactional
    public void delete(Long id) {
        Book book = getById(id);
        if (book.getCopiesOnLoan() > 0 || loans.existsByBookAndReturnDateIsNull(book)) {
            throw new IllegalStateException(
                    "Cannot remove \"" + book.getTitle() + "\": "
                            + book.getCopiesOnLoan() + " copies are on loan.");
        }
        waitlist.deleteByBook(book);
        loans.deleteByBook(book);
        books.delete(book);
    }

    /** The fields a librarian is allowed to set, and only those. */
    private void applyForm(Book book, BookForm form) {
        book.setIsbn(form.getIsbn().trim());
        book.setTitle(form.getTitle().trim());
        book.setAuthor(form.getAuthor().trim());
        book.setCategory(form.getCategory() == null || form.getCategory().isBlank()
                ? null
                : form.getCategory().trim());
        book.setTotalCopies(form.getTotalCopies());
    }
}
