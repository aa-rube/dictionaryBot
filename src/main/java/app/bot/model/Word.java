package app.bot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "word")
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String word;

    public Word() {
    }

    public Word(String word) {
        this.word = word;
    }

    public int getId() {
        return id;
    }

    public String getWord() {
        return word;
    }
}
