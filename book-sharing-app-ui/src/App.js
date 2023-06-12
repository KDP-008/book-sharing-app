import React, { useEffect, useState } from 'react';
import logo from './logo.svg';
import './App.css';

const App = () => {

  const [books, setBooks] = useState([]);

  useEffect(() => {

    fetch('api/book/v1/getAllBooks')
      .then(response => response.json())
      .then(data => {
        setBooks(data);
      })
  }, []);

return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <div className="App-intro">
          <h2>Book List</h2>
          {books.map(book =>
            <div key={book.bookId}>
              {book.name}
            </div>
          )}
        </div>
      </header>
    </div>
  );
 }

export default App;
