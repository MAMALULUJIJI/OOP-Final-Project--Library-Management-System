package com.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.library.domain.Book;
import com.library.repository.BookRepository;

/**
 * Loads a large catalog of sample titles when the database is empty, so the
 * demo and local browsing aren't stuck on a blank shelf. Skips entirely once
 * any book exists — librarians keep control after that.
 */
@Component
@Order(2)
public class BookSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BookSeeder.class);

    private final BookRepository books;

    public BookSeeder(BookRepository books) {
        this.books = books;
    }

    @Override
    public void run(String... args) {
        if (books.count() > 0) {
            return;
        }

        String[][] catalog = {
            // Fiction
            {"9780141439518", "Pride and Prejudice", "Jane Austen", "Fiction", "4"},
            {"9780141439600", "Jane Eyre", "Charlotte Brontë", "Fiction", "3"},
            {"9780141439570", "Wuthering Heights", "Emily Brontë", "Fiction", "3"},
            {"9780141439846", "Great Expectations", "Charles Dickens", "Fiction", "4"},
            {"9780141439556", "Frankenstein", "Mary Shelley", "Fiction", "5"},
            {"9780141441146", "Dracula", "Bram Stoker", "Fiction", "3"},
            {"9780141439563", "The Picture of Dorian Gray", "Oscar Wilde", "Fiction", "3"},
            {"9780141442464", "Crime and Punishment", "Fyodor Dostoevsky", "Fiction", "4"},
            {"9780140449266", "Anna Karenina", "Leo Tolstoy", "Fiction", "3"},
            {"9780140449136", "War and Peace", "Leo Tolstoy", "Fiction", "2"},
            {"9780141187761", "1984", "George Orwell", "Fiction", "6"},
            {"9780141182704", "Animal Farm", "George Orwell", "Fiction", "5"},
            {"9780141182636", "Brave New World", "Aldous Huxley", "Fiction", "4"},
            {"9780141187762", "To Kill a Mockingbird", "Harper Lee", "Fiction", "5"},
            {"9780142437230", "Moby-Dick", "Herman Melville", "Fiction", "2"},
            {"9780141439601", "The Great Gatsby", "F. Scott Fitzgerald", "Fiction", "6"},
            {"9780141187763", "Beloved", "Toni Morrison", "Fiction", "3"},
            {"9780141182637", "One Hundred Years of Solitude", "Gabriel García Márquez", "Fiction", "3"},
            {"9780140449267", "The Metamorphosis", "Franz Kafka", "Fiction", "4"},
            {"9780141187764", "The Catcher in the Rye", "J. D. Salinger", "Fiction", "4"},
            {"9780141187765", "Lord of the Flies", "William Golding", "Fiction", "4"},
            {"9780141187766", "The Handmaid's Tale", "Margaret Atwood", "Fiction", "3"},
            {"9780141187767", "Things Fall Apart", "Chinua Achebe", "Fiction", "3"},
            {"9780141187768", "Invisible Man", "Ralph Ellison", "Fiction", "2"},
            {"9780141187769", "The Color Purple", "Alice Walker", "Fiction", "3"},
            {"9780141439617", "Emma", "Jane Austen", "Fiction", "3"},
            {"9780141439623", "Sense and Sensibility", "Jane Austen", "Fiction", "3"},
            {"9780141187770", "The Road", "Cormac McCarthy", "Fiction", "3"},
            {"9780141187771", "Never Let Me Go", "Kazuo Ishiguro", "Fiction", "3"},
            {"9780141187772", "The Kite Runner", "Khaled Hosseini", "Fiction", "4"},

            // History
            {"9780143034759", "The Guns of August", "Barbara W. Tuchman", "History", "3"},
            {"9780143118206", "SPQR: A History of Ancient Rome", "Mary Beard", "History", "3"},
            {"9780143127550", "Sapiens", "Yuval Noah Harari", "History", "5"},
            {"9780143127741", "Homo Deus", "Yuval Noah Harari", "History", "3"},
            {"9780143038092", "A People's History of the United States", "Howard Zinn", "History", "3"},
            {"9780143116394", "The Warmth of Other Suns", "Isabel Wilkerson", "History", "3"},
            {"9780143127551", "Team of Rivals", "Doris Kearns Goodwin", "History", "2"},
            {"9780143038245", "The Rise and Fall of the Third Reich", "William L. Shirer", "History", "2"},
            {"9780143127552", "1491", "Charles C. Mann", "History", "3"},
            {"9780143127553", "1493", "Charles C. Mann", "History", "2"},
            {"9780143118213", "The Silk Roads", "Peter Frankopan", "History", "3"},
            {"9780143038246", "Genghis Khan and the Making of the Modern World", "Jack Weatherford", "History", "3"},
            {"9780143127554", "The Wright Brothers", "David McCullough", "History", "3"},
            {"9780143038247", "1776", "David McCullough", "History", "3"},
            {"9780143127555", "The Devil in the White City", "Erik Larson", "History", "4"},
            {"9780143118220", "Empire of the Summer Moon", "S. C. Gwynne", "History", "2"},
            {"9780143038248", "The Diary of a Young Girl", "Anne Frank", "History", "5"},
            {"9780143127556", "Bury My Heart at Wounded Knee", "Dee Brown", "History", "2"},
            {"9780143118237", "The History of the Ancient World", "Susan Wise Bauer", "History", "2"},
            {"9780143038249", "Guns, Germs, and Steel", "Jared Diamond", "History", "4"},

            // Science
            {"9780553380163", "A Brief History of Time", "Stephen Hawking", "Science", "4"},
            {"9780553380164", "The Selfish Gene", "Richard Dawkins", "Science", "3"},
            {"9780553380165", "Cosmos", "Carl Sagan", "Science", "4"},
            {"9780553380166", "The Origin of Species", "Charles Darwin", "Science", "3"},
            {"9780553380167", "The Double Helix", "James D. Watson", "Science", "3"},
            {"9780553380168", "Surely You're Joking, Mr. Feynman!", "Richard P. Feynman", "Science", "3"},
            {"9780553380169", "The Elegant Universe", "Brian Greene", "Science", "3"},
            {"9780553380170", "Astrophysics for People in a Hurry", "Neil deGrasse Tyson", "Science", "5"},
            {"9780553380171", "The Gene", "Siddhartha Mukherjee", "Science", "3"},
            {"9780553380172", "The Emperor of All Maladies", "Siddhartha Mukherjee", "Science", "3"},
            {"9780553380173", "Thinking, Fast and Slow", "Daniel Kahneman", "Science", "4"},
            {"9780553380174", "The Immortal Life of Henrietta Lacks", "Rebecca Skloot", "Science", "4"},
            {"9780553380175", "Silent Spring", "Rachel Carson", "Science", "3"},
            {"9780553380176", "The Structure of Scientific Revolutions", "Thomas S. Kuhn", "Science", "2"},
            {"9780553380177", "A Short History of Nearly Everything", "Bill Bryson", "Science", "4"},
            {"9780553380178", "The Hidden Life of Trees", "Peter Wohlleben", "Science", "3"},
            {"9780553380179", "The Body", "Bill Bryson", "Science", "3"},
            {"9780553380180", "The Sixth Extinction", "Elizabeth Kolbert", "Science", "3"},
            {"9780553380181", "Why We Sleep", "Matthew Walker", "Science", "4"},
            {"9780553380182", "The Demon-Haunted World", "Carl Sagan", "Science", "3"},

            // Philosophy
            {"9780140449334", "The Republic", "Plato", "Philosophy", "4"},
            {"9780140449198", "Meditations", "Marcus Aurelius", "Philosophy", "5"},
            {"9780140449181", "Nicomachean Ethics", "Aristotle", "Philosophy", "3"},
            {"9780140449211", "Beyond Good and Evil", "Friedrich Nietzsche", "Philosophy", "3"},
            {"9780140449228", "Thus Spoke Zarathustra", "Friedrich Nietzsche", "Philosophy", "3"},
            {"9780140449235", "The Symposium", "Plato", "Philosophy", "3"},
            {"9780140449242", "Critique of Pure Reason", "Immanuel Kant", "Philosophy", "2"},
            {"9780140449259", "Being and Time", "Martin Heidegger", "Philosophy", "2"},
            {"9780140449260", "The Myth of Sisyphus", "Albert Camus", "Philosophy", "4"},
            {"9780140449277", "Existentialism Is a Humanism", "Jean-Paul Sartre", "Philosophy", "3"},
            {"9780140449284", "The Prince", "Niccolò Machiavelli", "Philosophy", "4"},
            {"9780140449291", "Leviathan", "Thomas Hobbes", "Philosophy", "2"},
            {"9780140449307", "On Liberty", "John Stuart Mill", "Philosophy", "3"},
            {"9780140449314", "The Social Contract", "Jean-Jacques Rousseau", "Philosophy", "3"},
            {"9780140449321", "Tao Te Ching", "Laozi", "Philosophy", "4"},
            {"9780140449338", "The Analects", "Confucius", "Philosophy", "3"},
            {"9780140449345", "Letters from a Stoic", "Seneca", "Philosophy", "4"},
            {"9780140449352", "Discourses and Selected Writings", "Epictetus", "Philosophy", "3"},
            {"9780140449369", "The Consolation of Philosophy", "Boethius", "Philosophy", "2"},
            {"9780140449376", "A History of Western Philosophy", "Bertrand Russell", "Philosophy", "2"},

            // Poetry
            {"9780140424393", "Leaves of Grass", "Walt Whitman", "Poetry", "3"},
            {"9780140424409", "The Complete Poems", "Emily Dickinson", "Poetry", "3"},
            {"9780140424416", "The Waste Land and Other Poems", "T. S. Eliot", "Poetry", "3"},
            {"9780140424423", "Selected Poems", "Langston Hughes", "Poetry", "3"},
            {"9780140424430", "The Odyssey", "Homer", "Poetry", "4"},
            {"9780140424447", "The Iliad", "Homer", "Poetry", "3"},
            {"9780140424454", "Paradise Lost", "John Milton", "Poetry", "2"},
            {"9780140424461", "The Divine Comedy", "Dante Alighieri", "Poetry", "2"},
            {"9780140424478", "Sonnets", "William Shakespeare", "Poetry", "4"},
            {"9780140424485", "The Collected Poems", "W. B. Yeats", "Poetry", "2"},
            {"9780140424492", "Ariel", "Sylvia Plath", "Poetry", "3"},
            {"9780140424508", "The Complete Poems", "John Keats", "Poetry", "3"},
            {"9780140424515", "Songs of Innocence and of Experience", "William Blake", "Poetry", "3"},
            {"9780140424522", "Howl and Other Poems", "Allen Ginsberg", "Poetry", "3"},
            {"9780140424539", "The Collected Poems of W. H. Auden", "W. H. Auden", "Poetry", "2"},
            {"9780140424546", "Milk and Honey", "Rupi Kaur", "Poetry", "5"},
            {"9780140424553", "The Sun and Her Flowers", "Rupi Kaur", "Poetry", "4"},
            {"9780140424560", "Citizen: An American Lyric", "Claudia Rankine", "Poetry", "3"},
            {"9780140424577", "Selected Poems", "Rumi", "Poetry", "4"},
            {"9780140424584", "Twenty Love Poems and a Song of Despair", "Pablo Neruda", "Poetry", "3"},
        };

        for (String[] row : catalog) {
            Book book = new Book();
            book.setIsbn(row[0]);
            book.setTitle(row[1]);
            book.setAuthor(row[2]);
            book.setCategory(row[3]);
            int copies = Integer.parseInt(row[4]);
            book.setTotalCopies(copies);
            book.setAvailableCopies(copies);
            books.save(book);
        }

        log.info("Seeded {} sample books into an empty catalog.", catalog.length);
    }
}
