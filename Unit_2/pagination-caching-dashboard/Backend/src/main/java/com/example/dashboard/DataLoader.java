package com.example.dashboard;

import com.example.dashboard.entity.Author;
import com.example.dashboard.entity.Book;
import com.example.dashboard.repository.AuthorRepository;
import com.example.dashboard.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds a reasonably "large" dataset (60 authors, ~1200 books) on startup so
 * pagination, sorting, N+1 vs JOIN FETCH, and caching are all visible with
 * realistic numbers instead of a handful of rows.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final Random random = new Random(42);

    private static final String[] COUNTRIES = {
            "USA", "UK", "India", "Canada", "Australia", "Germany", "France", "Japan", "Brazil", "Kenya"
    };
    private static final String[] FIRST_NAMES = {
            "Ava", "Liam", "Noah", "Emma", "Oliver", "Sophia", "Elijah", "Mia", "James", "Amara",
            "Ravi", "Priya", "Wei", "Yuki", "Carlos", "Fatima", "Hana", "Diego", "Zoe", "Leo"
    };
    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Kumar", "Chen", "Garcia", "Muller", "Dubois", "Tanaka", "Silva", "Kamau",
            "Brown", "Davis", "Patel", "Wang", "Rossi", "Andersen", "Novak", "Kim", "Nguyen", "Oconnor"
    };
    private static final String[] GENRES = {
            "Fiction", "Non-Fiction", "Science Fiction", "Fantasy", "Mystery",
            "Biography", "History", "Technology", "Self-Help", "Romance", "Popularity"
    };
    private static final String[] TITLE_WORDS = {
            "Shadow", "Journey", "Silent", "Echo", "Rising", "Beyond", "Legacy", "Whisper", "Horizon",
            "Code", "Empire", "Garden", "Storm", "Light", "River", "Mirror", "Kingdom", "Ashes", "Signal", "Dawn"
    };

    public DataLoader(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) {
        if (authorRepository.count() > 0) {
            return; // already seeded
        }

        List<Author> authors = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            String name = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
            authors.add(new Author(name, country));
        }
        authors = authorRepository.saveAll(authors);

        List<Book> books = new ArrayList<>();
        for (int i = 0; i < 1200; i++) {
            Author author = authors.get(random.nextInt(authors.size()));
            String title = TITLE_WORDS[random.nextInt(TITLE_WORDS.length)] + " of the "
                    + TITLE_WORDS[random.nextInt(TITLE_WORDS.length)];
            String genre = GENRES[random.nextInt(GENRES.length)];
            int year = 1980 + random.nextInt(45);
            double price = 5 + Math.round(random.nextDouble() * 4500) / 100.0;
            double popularity = Math.round((3.5 + random.nextDouble() * 1.5) * 10.0) / 10.0;
            books.add(new Book(title, genre, year, price, popularity, author));
        }
        bookRepository.saveAll(books);

        System.out.println("Seeded " + authors.size() + " authors and " + books.size() + " books.");
    }
}
