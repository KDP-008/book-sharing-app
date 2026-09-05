-- Sample users
INSERT INTO users (name, email, password, favorite_genre, favorite_author) VALUES
('Alice', 'alice@example.com', 'password123', 'Fantasy', 'J.R.R. Tolkien');

INSERT INTO users (name, email, password, favorite_genre, favorite_author) VALUES
('Bob', 'bob@example.com', 'password123', 'Science Fiction', 'Isaac Asimov');

-- Sample books (owner_id refers to users inserted above)
INSERT INTO books (title, author, genre, available, owner_id) VALUES
('The Hobbit', 'J.R.R. Tolkien', 'Fantasy', TRUE, 1);

INSERT INTO books (title, author, genre, available, owner_id) VALUES
('Foundation', 'Isaac Asimov', 'Science Fiction', TRUE, 2);

-- Sample borrowing record: Bob borrowed The Hobbit from Alice
INSERT INTO borrowing_records (book_id, borrower_id, owner_id, borrow_date, due_date, return_date, delivery_method, active) VALUES
(1, 2, 1, '2026-09-01', '2026-09-15', NULL, 'COURIER', TRUE);
